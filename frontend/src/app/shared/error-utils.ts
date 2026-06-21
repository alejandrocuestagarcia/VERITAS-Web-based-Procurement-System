export function extractErrorMessage(err: any, fallback: string = 'An error occurred'): string {
  const message = err?.error?.message;
  console.log(message);
  if (message && typeof message === 'string') {
    return `${fallback}:\n${message}`;
  }

  return fallback;
}
