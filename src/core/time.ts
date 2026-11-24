export function getTodayDateString(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatDateDisplay(dateStr: string): string {
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  });
}

export function formatShortDate(dateStr: string): string {
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric'
  });
}

export function formatTimeDisplay(timeStr: string, is24Hour = false): string {
  if (!timeStr) return '';
  const [hStr, mStr] = timeStr.split(':');
  const h = parseInt(hStr, 10);
  const m = parseInt(mStr, 10);
  if (isNaN(h) || isNaN(m)) return timeStr;

  if (is24Hour) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  }
  const period = h >= 12 ? 'PM' : 'AM';
  const displayH = h % 12 === 0 ? 12 : h % 12;
  return `${String(displayH).padStart(2, '0')}:${String(m).padStart(2, '0')} ${period}`;
}

export interface DayInfo {
  dateString: string;
  dayNumber: number;
  dayName: string; // 'MON', 'TUE', etc.
  dayNameShort: string; // 'M', 'T', 'W', etc.
  isToday: boolean;
}

export function getWeekDates(centerDateString = getTodayDateString(), weekStartDay = 1): DayInfo[] {
  // weekStartDay: 1 = Monday, 0 = Sunday
  const [y, m, d] = centerDateString.split('-').map(Number);
  const center = new Date(y, m - 1, d);
  const todayStr = getTodayDateString();

  // Find Monday of the current week
  const dayOfWeek = center.getDay(); // 0 is Sunday, 1 is Monday
  const diffToStart = (dayOfWeek - weekStartDay + 7) % 7;
  const startOfWeek = new Date(center);
  startOfWeek.setDate(center.getDate() - diffToStart);

  const days: DayInfo[] = [];
  const dayNames = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

  for (let i = 0; i < 7; i++) {
    const cur = new Date(startOfWeek);
    cur.setDate(startOfWeek.getDate() + i);

    const year = cur.getFullYear();
    const month = String(cur.getMonth() + 1).padStart(2, '0');
    const day = String(cur.getDate()).padStart(2, '0');
    const dateStr = `${year}-${month}-${day}`;

    days.push({
      dateString: dateStr,
      dayNumber: cur.getDate(),
      dayName: dayNames[cur.getDay()],
      dayNameShort: dayNames[cur.getDay()].slice(0, 3),
      isToday: dateStr === todayStr
    });
  }

  return days;
}

export function getMonthMatrix(year: number, month: number, weekStartDay = 1) {
  // month: 0-indexed (0 = Jan, 11 = Dec)
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const daysInMonth = lastDay.getDate();

  const startDayOfWeek = firstDay.getDay(); // 0-6
  const leadDays = (startDayOfWeek - weekStartDay + 7) % 7;

  const matrix: { dateString: string; dayNumber: number; inMonth: boolean }[] = [];

  // Previous month trailing days
  const prevMonthLastDay = new Date(year, month, 0).getDate();
  for (let i = leadDays - 1; i >= 0; i--) {
    const d = prevMonthLastDay - i;
    const prevDate = new Date(year, month - 1, d);
    const dateStr = `${prevDate.getFullYear()}-${String(prevDate.getMonth() + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    matrix.push({ dateString: dateStr, dayNumber: d, inMonth: false });
  }

  // Current month days
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    matrix.push({ dateString: dateStr, dayNumber: d, inMonth: true });
  }

  // Next month leading days to complete grid (multiples of 7)
  const remaining = (7 - (matrix.length % 7)) % 7;
  for (let d = 1; d <= remaining; d++) {
    const nextDate = new Date(year, month + 1, d);
    const dateStr = `${nextDate.getFullYear()}-${String(nextDate.getMonth() + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    matrix.push({ dateString: dateStr, dayNumber: d, inMonth: false });
  }

  return matrix;
}
