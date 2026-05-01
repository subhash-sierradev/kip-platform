<#-- ============================================================
     monitoring_data_report.ftl
     Confluence Storage Format Template
     Root model : ConfluenceMonitoringReport
     UI labels  : section.uiLabels["key"] — fully data-driven,
                  translated by KwMonitoringDataTranslator.translateLabelMaps().
                  No hardcoded translated strings in this file.
     ============================================================ -->

<#-- ══════════════════════════════════════════════════════════
     PRIORITY COLOUR / STYLE HELPERS  (language-independent)
     ══════════════════════════════════════════════════════════ -->

<#function priorityColour p>
  <#switch p?upper_case>
    <#case "CRITICAL"> <#return "Red"/>    <#break>
    <#case "HIGH">     <#return "Red"/>    <#break>
    <#case "MEDIUM">   <#return "Yellow"/> <#break>
    <#case "LOW">      <#return "Green"/>  <#break>
    <#case "INFO">     <#return "Blue"/>   <#break>
    <#default>           <#return "Grey"/>
  </#switch>
</#function>

<#function priorityBorderHex p>
  <#switch p?upper_case>
    <#case "CRITICAL"> <#return "#F87462"/> <#break>
    <#case "HIGH">     <#return "#F87462"/> <#break>
    <#case "MEDIUM">   <#return "#FAA53D"/> <#break>
    <#case "LOW">      <#return "#4BCE97"/> <#break>
    <#case "INFO">     <#return "#579DFF"/> <#break>
    <#default>           <#return "#DCDFE4"/>
  </#switch>
</#function>

<#function priorityBgHex p>
  <#switch p?upper_case>
    <#case "CRITICAL"> <#return "#FFECEB"/> <#break>
    <#case "HIGH">     <#return "#FFECEB"/> <#break>
    <#case "MEDIUM">   <#return "#FFF4E5"/> <#break>
    <#case "LOW">      <#return "#DCFFF1"/> <#break>
    <#case "INFO">     <#return "#E9F2FF"/> <#break>
    <#default>           <#return "#F7F8F9"/>
  </#switch>
</#function>

<#function priorityEmoji p>
  <#switch p?upper_case>
    <#case "CRITICAL"> <#return "🔴"/> <#break>
    <#case "HIGH">     <#return "🔴"/> <#break>
    <#case "MEDIUM">   <#return "🟡"/> <#break>
    <#case "LOW">      <#return "🟢"/> <#break>
    <#case "INFO">     <#return "🔵"/> <#break>
    <#default>           <#return "⚪"/>
  </#switch>
</#function>

<#function priorityTextHex p>
  <#switch p?upper_case>
    <#case "CRITICAL"> <#return "#AE2A19"/> <#break>
    <#case "HIGH">     <#return "#AE2A19"/> <#break>
    <#case "MEDIUM">   <#return "#974F0C"/> <#break>
    <#case "LOW">      <#return "#216E4E"/> <#break>
    <#case "INFO">     <#return "#0055CC"/> <#break>
    <#default>           <#return "#626F86"/>
  </#switch>
</#function>

<#-- ══════════════════════════════════════════════════════════
     REUSABLE MACROS
     ══════════════════════════════════════════════════════════ -->

<#macro badge label colour><ac:structured-macro ac:name="status" ac:schema-version="1"><ac:parameter ac:name="colour">${colour}</ac:parameter><ac:parameter ac:name="title">${label?xml}</ac:parameter></ac:structured-macro></#macro>

<#macro row heading>
          <tr>
            <th style="width:210px;background:#F7F8F9;padding:8px 12px;vertical-align:top;">${heading?xml}</th>
            <td style="padding:8px 12px;vertical-align:top;"><#nested></td>
          </tr>
</#macro>

<#macro bulletList values code=false>
<ul style="margin:0;padding-left:18px;">
  <#list values as value>
    <#if value?trim?has_content>
    <li style="margin:2px 0;">
      <#if code><code>${value?trim?xml}</code><#else>${value?trim?xml}</#if>
    </li>
    </#if>
  </#list>
</ul>
</#macro>

<#function cleanupDisplayValue raw>
  <#assign val = raw?string?trim/>
  <#if !val?has_content>
    <#return ""/>
  </#if>
  <#assign val = val?replace("^\"|\"$", "", "r")/>
  <#assign val = val?replace("_", " ")/>
  <#return val/>
</#function>

<#function displayOrDash value>
  <#assign txt = (value!"")?string?trim/>
  <#if txt?has_content>
    <#return txt/>
  </#if>
  <#return "-"/>
</#function>

<#macro renderHtmlOrDash value>
  <#assign txt = (value!"")?string?trim/>
  <#if txt?has_content>
    ${value}
  <#else>
    -
  </#if>
</#macro>

<#function extractStructuredValues raw>
  <#assign source = (raw!"")?string?trim/>
  <#assign values = []/>
  <#if !source?has_content>
    <#return values/>
  </#if>
  <#assign normalized = source?replace("^\\[|\\]$", "", "r")/>
  <#if normalized?contains("title=")>
    <#assign parts = normalized?split("title=")/>
    <#if parts?size gt 1>
      <#list 1..(parts?size - 1) as idx>
        <#assign candidate = parts[idx]?keep_before("}")?keep_before(",")?trim/>
        <#if candidate?has_content>
          <#assign values = values + [cleanupDisplayValue(candidate)]/>
        </#if>
      </#list>
    </#if>
  <#elseif normalized?contains("label=")>
    <#assign parts = normalized?split("label=")/>
    <#if parts?size gt 1>
      <#list 1..(parts?size - 1) as idx>
        <#assign candidate = parts[idx]?keep_before("}")?keep_before(",")?trim/>
        <#if candidate?has_content>
          <#assign values = values + [cleanupDisplayValue(candidate)]/>
        </#if>
      </#list>
    </#if>
  <#elseif normalized?contains("name=")>
    <#assign parts = normalized?split("name=")/>
    <#if parts?size gt 1>
      <#list 1..(parts?size - 1) as idx>
        <#assign candidate = parts[idx]?keep_before("}")?keep_before(",")?trim/>
        <#if candidate?has_content>
          <#assign values = values + [cleanupDisplayValue(candidate)]/>
        </#if>
      </#list>
    </#if>
  </#if>
  <#if !values?has_content>
    <#if normalized?contains("=")>
      <#list normalized?replace("[", "")?replace("]", "")?replace("{", "")?replace("}", "")?split(",") as token>
        <#assign pair = token?trim/>
        <#if pair?contains("=")>
          <#assign key = pair?keep_before("=")?trim?lower_case/>
          <#assign candidate = cleanupDisplayValue(pair?keep_after("="))/>
          <#if candidate?has_content && key != "entityid" && key != "id" && key != "entitytype" && key != "type">
            <#assign values = values + [candidate]/>
          </#if>
        </#if>
      </#list>
    </#if>
  </#if>
  <#if !values?has_content>
    <#if normalized?contains(",")>
      <#list normalized?split(",") as part>
        <#assign cleaned = cleanupDisplayValue(part?replace("^\\{", "", "r")?replace("\\}$", "", "r"))/>
        <#if cleaned?has_content && !cleaned?contains("=")>
          <#assign values = values + [cleaned]/>
        </#if>
      </#list>
    <#else>
      <#assign cleaned = cleanupDisplayValue(normalized?replace("^\\{", "", "r")?replace("\\}$", "", "r"))/>
      <#if cleaned?has_content && !cleaned?contains("=")>
        <#assign values = [cleaned]/>
      </#if>
    </#if>
  </#if>
  <#return values/>
</#function>

<#macro renderCleanField raw forceList=false>
  <#assign cleanedValues = extractStructuredValues(raw)/>
  <#assign rawText = (raw!"")?string?trim/>
  <#if cleanedValues?has_content>
    <#if forceList || cleanedValues?size gt 1>
      <@bulletList values=cleanedValues/>
    <#else>
      ${cleanedValues[0]?xml}
    </#if>
  <#else>
    <#if !rawText?has_content>
      -
    <#elseif rawText?contains("=") || rawText?contains("{") || rawText?contains("[")>
      -
    <#else>
      ${rawText?xml}
    </#if>
  </#if>
</#macro>

<#-- ══════════════════════════════════════════════════════════
     TABLE OF CONTENTS
     clientGroups : the ClientGroup list to iterate (English or translated)
     ══════════════════════════════════════════════════════════ -->
<#macro renderToc clientGroups>
<#assign tocTotal = 0/>
<#list clientGroups as cg>
  <#assign tocTotal = tocTotal + cg.totalReports/>
</#list>

<ac:structured-macro ac:name="expand" ac:schema-version="1">
  <ac:parameter ac:name="title">${model.clientCount} Client<#if model.clientCount != 1>s</#if>, ${tocTotal} Report<#if tocTotal != 1>s</#if></ac:parameter>
  <ac:rich-text-body>
<ul style="margin:4px 0;padding-left:16px;">
<#list clientGroups as cg>
  <li style="list-style-type:none;margin-top:10px;margin-bottom:4px;"><strong style="color:#1B3A6B;">📁 ${cg.clientName?xml}</strong></li>
  <li style="list-style-type:none;padding-left:0;margin:0;">
    <ul style="margin-top:2px;margin-bottom:4px;">
    <#list cg.reports as r>
      <#assign caseTitle = ""/>
      <#if r.serialNumbers?? && r.serialNumbers?has_content>
        <#assign caseTitle = r.serialNumbers?join(", ")/>
      </#if>
      <#assign hl = r.title?xml/>
      <#if r.title?length gt 70><#assign hl = r.title[0..69]?xml + "…"/></#if>
      <li style="margin-bottom:2px;">${priorityEmoji(r.priority)} <#if caseTitle?has_content>${caseTitle?xml} — </#if>${hl}</li>
    </#list>
    </ul>
  </li>
</#list>
</ul>
  </ac:rich-text-body>
</ac:structured-macro>
</#macro>

<#-- ══════════════════════════════════════════════════════════
     SINGLE REPORT BLOCK
     uiLabels — Map<String,String> from LanguageSection.uiLabels().
       • Structural headings: lowercase keys  e.g. uiLabels["summary"]
       • Priority values    : UPPERCASE keys  e.g. uiLabels["HIGH"]
       • Dynamic field keys : lowercase of dynamicData key
       • Fallback via the ! operator if a key is missing.
     ══════════════════════════════════════════════════════════ -->
<#macro renderReport report uiLabels>
<ac:structured-macro ac:name="expand" ac:schema-version="1">
  <#assign caseTitle = ""/>
  <#if report.serialNumbers?? && report.serialNumbers?has_content>
    <#assign caseTitle = report.serialNumbers?join(", ")/>
  </#if>
  <#assign priorityLabel = (uiLabels[report.priority?upper_case])!report.priority?upper_case/>
  <ac:parameter ac:name="title">${priorityEmoji(report.priority)} [${priorityLabel?xml}] ${(caseTitle?has_content)?then(caseTitle?xml + " — ", "")}${report.title?xml}</ac:parameter>
  <ac:rich-text-body>

    <h3>${report.title?xml}</h3>

    <#-- Extract "Date" and "Entities" from dynamic fields into dedicated rows -->
    <#assign dateValue = ""/>
    <#assign entitiesValue = ""/>
    <#assign remainingFields = []/>
    <#if report.dynamicFields?? && report.dynamicFields?has_content>
      <#list report.dynamicFields as field>
        <#assign labelLc = field.label?lower_case?trim/>
        <#if labelLc == "date">
          <#assign dateValue = field.value/>
        <#elseif labelLc == "entities">
          <#assign entitiesValue = field.value/>
        <#else>
          <#assign remainingFields = remainingFields + [field]/>
        </#if>
      </#list>
    </#if>

    <ac:structured-macro ac:name="panel" ac:schema-version="1">
      <ac:parameter ac:name="borderStyle">solid</ac:parameter>
      <ac:parameter ac:name="borderColor">${priorityBorderHex(report.priority)}</ac:parameter>
      <ac:parameter ac:name="titleBGColor">${priorityBgHex(report.priority)}</ac:parameter>
      <ac:parameter ac:name="title">${((uiLabels["report_details"])!"Report Details")?xml}</ac:parameter>
      <ac:rich-text-body>
        <table data-layout="full-width" style="width:100%;table-layout:fixed;">
          <tbody>

            <@row heading=(uiLabels["priority"])!"Priority"><@badge label=priorityLabel colour=priorityColour(report.priority)/></@row>

            <#if report.serialNumbers?? && report.serialNumbers?has_content>
            <@row heading=(uiLabels["case_numbers"])!"Case Numbers">
              <@bulletList values=report.serialNumbers code=true/>
            </@row>
            </#if>

            <@row heading=(uiLabels["date"])!"Date">${displayOrDash((dateValue?has_content)?then(dateValue, report.createdAt))?xml}</@row>

            <@row heading=(uiLabels["client"])!"Client">${displayOrDash((report.client?has_content && report.client != "Unknown")?then(report.client, (uiLabels["unknown"])!"Unknown"))?xml}</@row>

            <#if entitiesValue?has_content>
            <@row heading=(uiLabels["entities"])!"Entities">
              <@renderCleanField raw=entitiesValue forceList=true/>
            </@row>
            </#if>

            <@row heading=(uiLabels["summary"])!"Summary"><@renderHtmlOrDash value=report.body/></@row>

            <#if remainingFields?has_content>
              <#list remainingFields as field>
                <#-- Look up translated label by lowercase key; fall back to original label -->
                <@row heading=(uiLabels[field.label?lower_case])!field.label><@renderCleanField raw=field.value/></@row>
              </#list>
            </#if>

            <#if report.authors?? && report.authors?has_content>
            <@row heading=(uiLabels["authors"])!"Authors">
              <@bulletList values=report.authors/>
            </@row>
            </#if>

            <@row heading=(uiLabels["created"])!"Created">${displayOrDash(report.createdAt)?xml}</@row>
            <@row heading=(uiLabels["updated"])!"Updated">${displayOrDash(report.updatedAt)?xml}</@row>

          </tbody>
        </table>
      </ac:rich-text-body>
    </ac:structured-macro>

  </ac:rich-text-body>
</ac:structured-macro>
</#macro>

<#-- ══════════════════════════════════════════════════════════
     CLIENT GROUP BLOCK
     ══════════════════════════════════════════════════════════ -->
<#macro renderClientGroup group uiLabels>
<#assign displayClientName = (group.clientName?has_content && group.clientName != "Unknown")?then(group.clientName, (uiLabels["unknown"])!"Unknown")/>
<h2>${displayClientName?xml}</h2>

<ac:structured-macro ac:name="panel" ac:schema-version="1">
  <ac:parameter ac:name="title">${((uiLabels["client_summary"])!"Client Summary")?xml}</ac:parameter>
  <ac:parameter ac:name="borderStyle">solid</ac:parameter>
  <ac:parameter ac:name="borderColor">#DCDFE4</ac:parameter>
  <ac:parameter ac:name="titleBGColor">#F7F8F9</ac:parameter>
  <ac:rich-text-body>
    <p><strong>${((uiLabels["total_reports"])!"Total Reports")?xml}:</strong> ${group.totalReports}</p>
    <p style="margin:0;line-height:1.8;">
      <#list group.priorityCounts as pc>
        <#assign pcLabel = (uiLabels[pc.level])!pc.level/>
        <span style="display:inline-block;margin-right:8px;margin-bottom:6px;"><@badge label=pcLabel + " ×" + pc.count colour=pc.colour/></span>
      </#list>
    </p>
  </ac:rich-text-body>
</ac:structured-macro>

<#list group.reports as report>
<@renderReport report=report uiLabels=uiLabels/>
</#list>
</#macro>

<#-- ══════════════════════════════════════════════════════════
     PAGE BODY — one section per language (English first)
     ══════════════════════════════════════════════════════════ -->

<#list model.languageSections as section>
<h1>${section.languageDisplayName?xml}</h1>

<#-- ── Header info ───────────────────────────────────────────────────── -->
<ac:structured-macro ac:name="info" ac:schema-version="1">
  <ac:parameter ac:name="title">${((section.uiLabels["aggregated_daily_report"])!"Aggregated Daily Report")?xml} — ${model.reportDateLong?xml}</ac:parameter>
  <ac:rich-text-body>
    <p><strong>${((section.uiLabels["report_date_label"])!"Report Date")?xml}:</strong> ${model.reportDate?xml} &nbsp;|&nbsp; <strong>${((section.uiLabels["generated_label"])!"Generated")?xml}:</strong> ${model.generatedAt?xml} &nbsp;|&nbsp; <strong>${((section.uiLabels["total_reports"])!"Total Reports")?xml}:</strong> ${model.totalReports} &nbsp;|&nbsp; <strong>${((section.uiLabels["clients_label"])!"Clients")?xml}:</strong> ${model.clientCount}</p>
  </ac:rich-text-body>
</ac:structured-macro>

<#-- ── Priority summary ──────────────────────────────────────────────── -->
<ac:structured-macro ac:name="panel" ac:schema-version="1">
  <ac:parameter ac:name="title">${((section.uiLabels["priority_summary"])!"Priority Summary")?xml}</ac:parameter>
  <ac:parameter ac:name="borderStyle">solid</ac:parameter>
  <ac:parameter ac:name="borderColor">#DCDFE4</ac:parameter>
  <ac:parameter ac:name="titleBGColor">#F7F8F9</ac:parameter>
  <ac:rich-text-body>
    <table>
      <tbody>
        <tr>
          <#list section.prioritySummary as entry>
          <#assign levelLabel = (section.uiLabels[entry.level])!entry.level/>
          <th style="text-align:center;background:${priorityBgHex(entry.level)};color:${priorityTextHex(entry.level)};padding:8px 24px;border:1px solid ${priorityBorderHex(entry.level)};">${priorityEmoji(entry.level)} ${levelLabel?xml}</th>
          </#list>
        </tr>
        <tr>
          <#list section.prioritySummary as entry>
          <td style="text-align:center;color:${priorityTextHex(entry.level)};padding:12px;">${entry.count}</td>
          </#list>
        </tr>
      </tbody>
    </table>
  </ac:rich-text-body>
</ac:structured-macro>

<#-- ── Table of contents ─────────────────────────────────────────────── -->
<h2>📑 ${((section.uiLabels["table_of_contents"])!"Table of Contents")?xml}</h2>
<@renderToc clientGroups=section.clientGroups/>

<hr/>

<#list section.clientGroups as group>
<@renderClientGroup group=group uiLabels=section.uiLabels/>
<#if group?has_next><hr/></#if>
</#list>
<#if section?has_next><hr/><hr/></#if>
</#list>
