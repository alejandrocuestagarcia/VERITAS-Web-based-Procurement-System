export function extractErrorMessage(err: any, fallback: string = 'An error occurred'): string {
  const message = err?.error?.message;
  if (message && typeof message === 'string') {
    return `${message}`;
  }
  return fallback;
}
