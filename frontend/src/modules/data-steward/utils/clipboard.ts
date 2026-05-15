export async function copyText(text: string): Promise<boolean> {
  const value = text ?? '';
  if (!value.trim()) {
    return false;
  }

  if (navigator.clipboard?.writeText && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(value);
      return true;
    } catch {
      // Some embedded browsers expose clipboard but deny access. Fall back below.
    }
  }

  return copyTextWithTextarea(value);
}

function copyTextWithTextarea(text: string): boolean {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.setAttribute('readonly', 'true');
  textArea.style.position = 'fixed';
  textArea.style.left = '-9999px';
  textArea.style.top = '0';
  textArea.style.opacity = '0';

  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  textArea.setSelectionRange(0, textArea.value.length);

  try {
    return document.execCommand('copy');
  } catch {
    return false;
  } finally {
    document.body.removeChild(textArea);
  }
}
