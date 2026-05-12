/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'veritas-blue': 'var(--color-veritas-blue)',
        'veritas-dark': 'var(--color-veritas-dark)',
        'surface': 'var(--v-surface)',
        'surface-low': 'var(--v-surface-low)',
        'surface-highest': 'var(--v-surface-highest)',
        'surface-white': 'var(--v-surface-white)',
        'on-surface': 'var(--v-on-surface)',
        'muted-grey': 'var(--color-muted-grey)',
        'border-white': 'var(--v-border-white)',
        'border-ghost': 'var(--v-border-ghost)',
      },
      fontFamily: {
        manrope: ['Manrope', 'sans-serif'],
        inter: ['Inter', 'sans-serif'],
      },
      borderRadius: {
        'card': '14px',
        'chip': '20px',
        'search': '10px',
      },
      keyframes: {
        delayedFadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        }
      },
      animation: {
        //This waits 200ms then fades in over 100ms
        'delayed-fade': 'delayedFadeIn 0.1s ease-in 0.2s forwards',
      },
    },
  },
  plugins: [],
}
