/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: '#07120F',
        surface: {
          DEFAULT: '#101D18',
          elevated: '#172720',
          variant: '#20342B',
        },
        border: '#263A32',
        primary: {
          DEFAULT: '#A5E6D6',
          mint: '#A5E6D6',
          secondary: '#76CDBB',
          dark: '#0e3a30',
        },
        accent: {
          yellow: '#F0E99B',
          lavender: '#DCC3F5',
        },
        text: {
          primary: '#F1F5EF',
          secondary: '#A5B6AD',
          muted: '#71847A',
        },
        status: {
          error: '#F28D91',
          warning: '#F1C879',
          success: '#A5E6D6',
        }
      },
      borderRadius: {
        'card': '16px',
        'chip': '20px',
        'button': '12px',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      }
    },
  },
  plugins: [],
};
