import { describe, expect, it } from 'vitest';

import {
  capitalizeFirst,
  capitalizeName,
  getFirstName,
  getInitials,
  splitCamelCase,
  toPlainText,
} from '@/utils/stringFormatUtils';

describe('stringFormatUtils', () => {
  describe('splitCamelCase', () => {
    it('splits camelCase strings correctly', () => {
      expect(splitCamelCase('camelCaseString')).toBe('Camel Case String');
      expect(splitCamelCase('XMLHttpRequest')).toBe('Xml Http Request');
      expect(splitCamelCase('HTML5Parser')).toBe('Html 5 Parser');
    });

    it('handles single words', () => {
      expect(splitCamelCase('hello')).toBe('Hello');
      expect(splitCamelCase('HELLO')).toBe('Hello');
    });

    it('handles empty or invalid input', () => {
      expect(splitCamelCase('')).toBe('');
      expect(splitCamelCase('   ')).toBe('');
      // @ts-expect-error - testing invalid input
      expect(splitCamelCase(null)).toBe('');
      // @ts-expect-error - testing invalid input
      expect(splitCamelCase(undefined)).toBe('');
    });

    it('handles numbers and letters', () => {
      expect(splitCamelCase('test123ABC')).toBe('Test 123 Abc');
      expect(splitCamelCase('version2Beta')).toBe('Version 2 Beta');
    });
  });

  describe('getInitials', () => {
    it('gets initials from full name', () => {
      expect(getInitials('John Doe')).toBe('JD');
      expect(getInitials('Mary Jane Watson')).toBe('MJ');
      expect(getInitials('Single')).toBe('S');
    });

    it('handles empty or invalid input', () => {
      expect(getInitials('')).toBe('?');
      expect(getInitials('   ')).toBe('?');
      // @ts-expect-error - testing invalid input
      expect(getInitials(null)).toBe('?');
      // @ts-expect-error - testing invalid input
      expect(getInitials(undefined)).toBe('?');
    });

    it('handles extra whitespace', () => {
      expect(getInitials('  John   Doe  ')).toBe('JD');
      expect(getInitials('John    Middle   Last')).toBe('JM');
    });

    it('handles lowercase names', () => {
      expect(getInitials('john doe')).toBe('JD');
      expect(getInitials('mary jane')).toBe('MJ');
    });
  });

  describe('capitalizeName', () => {
    it('capitalizes names correctly', () => {
      expect(capitalizeName('john doe')).toBe('John Doe');
      expect(capitalizeName('MARY JANE WATSON')).toBe('Mary Jane Watson');
      expect(capitalizeName('single')).toBe('Single');
    });

    it('handles empty or invalid input', () => {
      expect(capitalizeName('')).toBe('');
      expect(capitalizeName('   ')).toBe('');
      // @ts-expect-error - testing invalid input
      expect(capitalizeName(null)).toBe('');
      // @ts-expect-error - testing invalid input
      expect(capitalizeName(undefined)).toBe('');
    });

    it('handles extra whitespace', () => {
      expect(capitalizeName('  john   doe  ')).toBe('John Doe');
      expect(capitalizeName('john    middle   last')).toBe('John Middle Last');
    });

    it('handles mixed case', () => {
      expect(capitalizeName('jOhN dOe')).toBe('John Doe');
      expect(capitalizeName('mArY jAnE')).toBe('Mary Jane');
    });
  });

  describe('capitalizeFirst', () => {
    it('capitalizes first letter only', () => {
      expect(capitalizeFirst('hello world')).toBe('Hello world');
      expect(capitalizeFirst('HELLO WORLD')).toBe('Hello world');
      expect(capitalizeFirst('h')).toBe('H');
    });

    it('handles empty or invalid input', () => {
      expect(capitalizeFirst('')).toBe('');
      expect(capitalizeFirst('   ')).toBe('');
      // @ts-expect-error - testing invalid input
      expect(capitalizeFirst(null)).toBe('');
      // @ts-expect-error - testing invalid input
      expect(capitalizeFirst(undefined)).toBe('');
    });

    it('handles leading/trailing whitespace', () => {
      expect(capitalizeFirst('  hello world  ')).toBe('Hello world');
    });
  });

  describe('getFirstName', () => {
    it('extracts first name correctly', () => {
      expect(getFirstName('John Doe')).toBe('John');
      expect(getFirstName('Mary Jane Watson')).toBe('Mary');
      expect(getFirstName('Single')).toBe('Single');
    });

    it('handles empty or invalid input', () => {
      expect(getFirstName('')).toBe('');
      expect(getFirstName('   ')).toBe('');
      // @ts-expect-error - testing invalid input
      expect(getFirstName(null)).toBe('');
      // @ts-expect-error - testing invalid input
      expect(getFirstName(undefined)).toBe('');
    });

    it('handles extra whitespace', () => {
      expect(getFirstName('  John   Doe  ')).toBe('John');
      expect(getFirstName('John    Middle   Last')).toBe('John');
    });

    it('handles case sensitivity', () => {
      expect(getFirstName('john doe')).toBe('john');
      expect(getFirstName('JOHN DOE')).toBe('JOHN');
    });
  });

  describe('toPlainText', () => {
    it('returns plain text for strings without HTML', () => {
      expect(toPlainText('Hello World')).toBe('Hello World');
      expect(toPlainText('Simple text')).toBe('Simple text');
    });

    it('strips HTML tags from text', () => {
      expect(toPlainText('<p>Hello World</p>')).toBe('Hello World');
      expect(toPlainText('<div><span>Test</span></div>')).toBe('Test');
      expect(toPlainText('<strong>Bold</strong> text')).toBe('Bold text');
    });

    it('handles HTML entities and complex markup', () => {
      expect(toPlainText('<p>Hello &amp; World</p>')).toBe('Hello & World');
      expect(toPlainText('<div>Line1<br>Line2</div>')).toBe('Line1Line2');
    });

    it('normalizes whitespace in HTML content', () => {
      expect(toPlainText('<p>  Multiple   spaces  </p>')).toBe('Multiple spaces');
      expect(toPlainText('<div>\n  Text\n  with\n  newlines\n</div>')).toContain('Text');
    });

    it('returns default value for empty input', () => {
      expect(toPlainText('')).toBe('');
      expect(toPlainText('', 'default')).toBe('default');
      expect(toPlainText('   ')).toBe('');
    });

    it('returns default value for non-string input', () => {
      // Testing invalid input - should return empty string or default
      expect(toPlainText(null as any)).toBe('');
      expect(toPlainText(undefined as any)).toBe('');
      expect(toPlainText(123 as any)).toBe('');
      expect(toPlainText({} as any, 'default')).toBe('default');
    });

    it('handles strings with only entities no tags', () => {
      expect(toPlainText('Hello &amp; goodbye')).toBe('Hello & goodbye');
      expect(toPlainText('Test &lt; 5')).toBe('Test < 5');
    });

    it('fast path returns text without HTML/entities', () => {
      expect(toPlainText('No HTML here')).toBe('No HTML here');
      expect(toPlainText('Simple string 123')).toBe('Simple string 123');
    });

    it('returns default when HTML parsing results in empty', () => {
      expect(toPlainText('<div></div>', 'empty')).toBe('empty');
      expect(toPlainText('<span>   </span>', 'blank')).toBe('blank');
    });

    it('handles nested HTML structures', () => {
      const nested = '<div><p><span>Deeply <strong>nested</strong></span> content</p></div>';
      expect(toPlainText(nested)).toBe('Deeply nested content');
    });

    it('handles self-closing tags', () => {
      expect(toPlainText('Line1<br/>Line2')).toBe('Line1Line2');
      expect(toPlainText('Image<img src="test.jpg"/>End')).toBe('ImageEnd');
    });

    it('falls back to regex stripping when DOMParser unavailable', () => {
      // Save original DOMParser
      const originalDOMParser = globalThis.DOMParser;

      // Mock DOMParser to be undefined
      // @ts-expect-error - intentionally making DOMParser unavailable
      delete globalThis.DOMParser;

      expect(toPlainText('<p>Fallback test</p>')).toBe('Fallback test');
      expect(toPlainText('<div>Regex <strong>strip</strong></div>')).toBe('Regex strip');

      // Restore DOMParser
      globalThis.DOMParser = originalDOMParser;
    });

    it('handles DOMParser throwing errors', () => {
      const originalDOMParser = globalThis.DOMParser;

      // Mock DOMParser to throw
      globalThis.DOMParser = class {
        parseFromString() {
          throw new Error('Parse error');
        }
      } as any;

      expect(toPlainText('<p>Error handling</p>')).toBe('Error handling');

      // Restore DOMParser
      globalThis.DOMParser = originalDOMParser;
    });
  });
});
