export const THEME_COLORS = {
  background: '#07120F',
  surface: '#101D18',
  surfaceElevated: '#172720',
  surfaceVariant: '#20342B',
  border: '#263A32',
  primary: '#A5E6D6',
  primarySecondary: '#76CDBB',
  accentYellow: '#F0E99B',
  accentLavender: '#DCC3F5',
  textPrimary: '#F1F5EF',
  textSecondary: '#A5B6AD',
  textMuted: '#71847A',
  error: '#F28D91',
  warning: '#F1C879',
  success: '#A5E6D6',
} as const;

export const CATEGORY_COLORS: Record<string, string> = {
  academics: '#A5E6D6', // Mint
  finance: '#F0E99B',   // Yellow
  fitness: '#DCC3F5',   // Lavender
  personal: '#76CDBB',  // Teal
  projects: '#F1C879',  // Gold
  work: '#93C5FD',      // Soft Blue
  other: '#A5B6AD',     // Gray
};
