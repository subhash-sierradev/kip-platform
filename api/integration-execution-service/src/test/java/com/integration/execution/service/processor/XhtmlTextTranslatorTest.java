package com.integration.execution.service.processor;

import com.integration.execution.client.TranslationApiClient;
import com.integration.execution.config.properties.TranslationApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XhtmlTextTranslator}.
 *
 * <p>Verifies the three core properties:
 * <ol>
 *   <li><em>Text-node isolation</em> — only human-readable text nodes are sent to the API;
 *       markup, {@code <ac:*>} attributes, and non-translatable text (hex colours, pure numbers)
 *       are excluded.</li>
 *   <li><em>Confluence colour guard</em> — content inside
 *       {@code <ac:parameter ac:name="colour">} is never translated.</li>
 *   <li><em>Graceful fallback</em> — if the API returns mismatched segments or fails entirely,
 *       the original XHTML is returned unchanged.</li>
 * </ol>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class XhtmlTextTranslatorTest {

    @Mock
    private TranslationApiClient translationApiClient;

    @Mock
    private TranslationApiProperties translationApiProperties;

    private XhtmlTextTranslator translator;

    @BeforeEach
    void setUp() {
        // Return 45 000 so the existing batch-overflow test keeps working with
        // "A".repeat(44_999). Lenient avoids "unnecessary stubbing" in tests that
        // never trigger the batch-limit check.
        lenient().when(translationApiProperties.getMaxChars()).thenReturn(45_000);
        translator = new XhtmlTextTranslator(translationApiClient, translationApiProperties);
    }

    // -----------------------------------------------------------------------
    // Null / blank input guard
    // -----------------------------------------------------------------------

    @Test
    void translateXhtml_whenNullInput_returnsNull() {
        String result = translator.translateXhtml(null, "en", "ja");

        assertThat(result).isNull();
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    void translateXhtml_whenBlankInput_returnsBlank() {
        String result = translator.translateXhtml("   ", "en", "ja");

        assertThat(result).isEqualTo("   ");
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // Text-node extraction & re-injection
    // -----------------------------------------------------------------------

    @Test
    void translateXhtml_simpleHtml_translatesTextNodeAndPreservesMarkup() {
        String input = "<p>Hello World</p>";
        when(translationApiClient.translate("Hello World", "en", "ja"))
                .thenReturn(Optional.of("こんにちは世界"));

        String result = translator.translateXhtml(input, "en", "ja");

        assertThat(result).contains("こんにちは世界");
        assertThat(result).contains("<p>");
    }

    @Test
    void translateXhtml_multipleTextNodes_batchedWithSeparatorAndRejected() {
        // Two text nodes → batched as "Node 1<<<SEG>>>Node 2"
        String input = "<div><p>First</p><p>Second</p></div>";
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(translationApiClient.translate(textCaptor.capture(), anyString(), anyString()))
                .thenReturn(Optional.of("一番目" + "\n" + XhtmlTextTranslator.SEG + "\n" + "二番目"));

        String result = translator.translateXhtml(input, "en", "ja");

        assertThat(result).contains("一番目");
        assertThat(result).contains("二番目");
        // The joined batch must contain the separator
        assertThat(textCaptor.getValue()).contains(XhtmlTextTranslator.SEG);
    }

    // -----------------------------------------------------------------------
    // Confluence colour guard (Consideration 1)
    // -----------------------------------------------------------------------

    @Test
    void translateXhtml_colourParameterContent_isNeverTranslated() {
        // The "Red" inside <ac:parameter ac:name="colour"> must NOT be translated.
        // The "HIGH" inside <ac:parameter ac:name="title"> is a regular parameter and SHOULD be.
        String input = "<ac:structured-macro ac:name=\"status\">"
                + "<ac:parameter ac:name=\"colour\">Red</ac:parameter>"
                + "<ac:parameter ac:name=\"title\">HIGH</ac:parameter>"
                + "</ac:structured-macro>";

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(translationApiClient.translate(textCaptor.capture(), anyString(), anyString()))
                .thenReturn(Optional.of("高"));

        translator.translateXhtml(input, "en", "ja");

        // The API must only see "HIGH", not "Red"
        assertThat(textCaptor.getValue()).doesNotContain("Red");
        assertThat(textCaptor.getValue()).contains("HIGH");
    }

    // -----------------------------------------------------------------------
    // Skip rules (Consideration 2)
    // -----------------------------------------------------------------------

    @Test
    void translateXhtml_hexColorText_isSkipped() {
        // A text node whose only content is a hex colour must not be translated.
        String input = "<span>#FF5630</span><p>Critical alert</p>";
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(translationApiClient.translate(textCaptor.capture(), anyString(), anyString()))
                .thenReturn(Optional.of("重大なアラート"));

        translator.translateXhtml(input, "en", "ja");

        assertThat(textCaptor.getValue()).doesNotContain("#FF5630");
        assertThat(textCaptor.getValue()).contains("Critical alert");
    }

    @Test
    void translateXhtml_pureNumberText_isSkipped() {
        String input = "<td>42</td><td>Summary text</td>";
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(translationApiClient.translate(textCaptor.capture(), anyString(), anyString()))
                .thenReturn(Optional.of("サマリーテキスト"));

        translator.translateXhtml(input, "en", "ja");

        assertThat(textCaptor.getValue()).doesNotContain("42");
        assertThat(textCaptor.getValue()).contains("Summary text");
    }

    @Test
    void translateXhtml_blankTextNodes_areSkipped() {
        // A document with only whitespace text nodes should not call the API.
        String input = "<div>   </div>";

        String result = translator.translateXhtml(input, "en", "ja");

        assertThat(result).isNotNull();
        verify(translationApiClient, never()).translate(anyString(), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // Graceful fallback (Consideration 3)
    // -----------------------------------------------------------------------

    @Test
    void translateXhtml_whenApiReturnsEmpty_returnsOriginalXhtml() {
        String input = "<p>Hello</p>";
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        String result = translator.translateXhtml(input, "en", "ja");

        assertThat(result).contains("Hello");
    }

    @Test
    void translateXhtml_whenApiReturnsMismatchedSegmentCount_returnsOriginalTexts() {
        // API returns 1 segment but we sent 2 text nodes — mismatch → keep originals.
        String input = "<div><p>First</p><p>Second</p></div>";
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Only one segment")); // no separator

        String result = translator.translateXhtml(input, "en", "ja");

        // Both original texts are preserved
        assertThat(result).contains("First");
        assertThat(result).contains("Second");
    }

    @Test
    void translateXhtml_whenApiThrows_returnsOriginalXhtml() {
        String input = "<p>Hello</p>";
        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Network error"));

        String result = translator.translateXhtml(input, "en", "ja");

        // Exception caught → original returned
        assertThat(result).contains("Hello");
    }

    @Test
    void translateXhtml_purePunctuationText_isSkipped() {
        // A text node made of only punctuation/dash chars must not be translated.
        String input = "<td>---</td><td>Summary text</td>";
        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        when(translationApiClient.translate(textCaptor.capture(), anyString(), anyString()))
                .thenReturn(Optional.of("サマリーテキスト"));

        translator.translateXhtml(input, "en", "ja");

        assertThat(textCaptor.getValue()).doesNotContain("---");
        assertThat(textCaptor.getValue()).contains("Summary text");
    }

    @Test
    void translateXhtml_textNodesExceedingBatchLimit_requiresMultipleApiCalls() {
        // Two text nodes each ~44 999 chars exceed the 45 000-char batch limit together,
        // so they must be sent in two separate API calls (covers the batch-split branch).
        String bigText1 = "A".repeat(44_999);
        String bigText2 = "B".repeat(44_999);
        String input = "<p>" + bigText1 + "</p><p>" + bigText2 + "</p>";

        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("T1"))
                .thenReturn(Optional.of("T2"));

        String result = translator.translateXhtml(input, "en", "ja");

        verify(translationApiClient, times(2)).translate(anyString(), anyString(), anyString());
        assertThat(result).contains("T1");
        assertThat(result).contains("T2");
    }

    @Test
    void translateXhtml_xmlCommentNode_isIgnoredAndTextIsTranslated() {
        // An XML comment is neither a TextNode nor an Element; it must be silently skipped
        // (covers the "neither TextNode nor Element" branch in collectTranslatableNodes).
        String input = "<p><!-- internal comment -->Translatable</p>";
        when(translationApiClient.translate("Translatable", "en", "ja"))
                .thenReturn(Optional.of("翻訳済み"));

        String result = translator.translateXhtml(input, "en", "ja");

        assertThat(result).contains("翻訳済み");
    }

    // -----------------------------------------------------------------------
    // Markup preservation
    // -----------------------------------------------------------------------

    @Test
    void translateXhtml_confluenceMacroAttributes_arePreservedExactly() {
        String input = "<ac:structured-macro ac:name=\"status\" ac:schema-version=\"1\">"
                + "<ac:parameter ac:name=\"colour\">Yellow</ac:parameter>"
                + "<ac:parameter ac:name=\"title\">MEDIUM</ac:parameter>"
                + "</ac:structured-macro>";

        when(translationApiClient.translate(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("中"));

        String result = translator.translateXhtml(input, "en", "ja");

        // Confluence macro structure and colour constant must be intact
        assertThat(result).contains("ac:name=\"status\"");
        assertThat(result).contains("ac:name=\"colour\"");
        assertThat(result).contains("Yellow"); // colour must NOT be translated
    }
}

