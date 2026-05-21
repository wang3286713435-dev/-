import DOMPurify from 'dompurify';
import { marked } from 'marked';

const FORBIDDEN_LINK_PATTERN = /(?:javascript:|file:|nas:\/\/|smb:\/\/|\/Volumes\/|\/Users\/|storage_path|storage_uri|token|secret|password|credential|bearer|api[_-]?key)/i;
const FORBIDDEN_TEXT_PATTERNS: Array<[RegExp, string]> = [
  [/\b(?:nas|smb):\/\/[^\s)]+/gi, '[路径已隐藏]'],
  [/\/Volumes\/[^\s)]+/g, '[路径已隐藏]'],
  [/\/Users\/[^\s)]+/g, '[路径已隐藏]'],
  [/\b(?:storage_path|storage_uri)\b\s*[:=]\s*[^\s,)]+/gi, '[内部字段已隐藏]'],
  [/\bBearer\s+[A-Za-z0-9._~+/=-]+/gi, '[敏感信息已隐藏]'],
  [/\b(?:token|secret|password|credential|api[_-]?key)\b\s*[:=]\s*[^\s,)]+/gi, '[敏感信息已隐藏]']
];

const SQL_LINE_PATTERN = /\b(select|insert|update|delete|drop|alter|create)\b[\s\S]{0,160}\b(from|into|table|set|where|values)\b/i;

marked.setOptions({
  async: false,
  breaks: false,
  gfm: true
});

export function renderSafeMarkdown(value: string) {
  const redacted = redactSensitiveMarkdown(escapeRawHtml(value));
  const rendered = marked.parse(redacted, { async: false }) as string;
  return sanitizeRenderedMarkdown(rendered);
}

export function redactSensitiveMarkdown(value: string) {
  const redacted = FORBIDDEN_TEXT_PATTERNS.reduce((text, [pattern, replacement]) => {
    return text.replace(pattern, replacement);
  }, value);

  return redacted
    .split('\n')
    .map((line) => (SQL_LINE_PATTERN.test(line) ? '[SQL已隐藏]' : line))
    .join('\n');
}

function escapeRawHtml(value: string) {
  return value.replace(/[<>]/g, (match) => (match === '<' ? '&lt;' : '&gt;'));
}

function sanitizeRenderedMarkdown(html: string) {
  const clean = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'a',
      'blockquote',
      'br',
      'code',
      'del',
      'em',
      'h1',
      'h2',
      'h3',
      'h4',
      'h5',
      'h6',
      'hr',
      'input',
      'li',
      'ol',
      'p',
      'pre',
      's',
      'strong',
      'table',
      'tbody',
      'td',
      'th',
      'thead',
      'tr',
      'ul'
    ],
    ALLOWED_ATTR: ['aria-hidden', 'checked', 'class', 'disabled', 'href', 'rel', 'target', 'title', 'type'],
    FORBID_TAGS: ['embed', 'form', 'iframe', 'img', 'math', 'object', 'script', 'style', 'svg', 'video'],
    RETURN_TRUSTED_TYPE: false
  });

  const template = document.createElement('template');
  template.innerHTML = clean;

  template.content.querySelectorAll('img, svg, iframe, object, embed, script, style, video').forEach((node) => {
    node.remove();
  });

  template.content.querySelectorAll('a').forEach((anchor) => {
    const href = anchor.getAttribute('href') ?? '';
    if (!isSafeLink(href)) {
      const replacement = document.createElement('span');
      replacement.textContent = `${anchor.textContent || '链接'}（链接已隐藏）`;
      anchor.replaceWith(replacement);
      return;
    }
    anchor.setAttribute('rel', 'noopener noreferrer');
    if (/^https?:\/\//i.test(href)) {
      anchor.setAttribute('target', '_blank');
    } else {
      anchor.removeAttribute('target');
    }
  });

  template.content.querySelectorAll('input[type="checkbox"]').forEach((input) => {
    input.setAttribute('disabled', 'true');
    input.setAttribute('aria-hidden', 'true');
  });

  return template.innerHTML;
}

function isSafeLink(href: string) {
  const normalized = href.trim();
  if (!normalized || FORBIDDEN_LINK_PATTERN.test(normalized)) return false;
  if (normalized.startsWith('#')) return true;
  if (normalized.startsWith('/') && !normalized.startsWith('//')) return true;
  return /^https?:\/\/[^\s]+$/i.test(normalized);
}
