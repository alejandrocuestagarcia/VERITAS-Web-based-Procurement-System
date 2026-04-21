/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'veritas-blue': '#0052CC',
        'veritas-dark': '#003D9B',
        'surface-grey': '#f7f9fb',
      }
    },
  },
  plugins: [],
}
