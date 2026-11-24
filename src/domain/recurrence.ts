import { Reminder } from './models';

export interface NextOccurrenceResult {
  date: string; // YYYY-MM-DD
  time: string; // HH:mm
  isoString: string;
}

/**
 * Pure recurrence calculator.
 * Calculates next occurrences without side effects.
 */
export class RecurrenceCalculator {
  /**
   * Calculate the next occurrence for a given reminder starting from `fromDate`.
   */
  static getNextOccurrence(reminder: Reminder, fromDate: Date = new Date()): NextOccurrenceResult | null {
    if (reminder.isPaused) return null;

    const time = reminder.targetTime || '09:00';
    const [hours, minutes] = time.split(':').map(Number);

    switch (reminder.type) {
      case 'ONE_TIME': {
        if (!reminder.targetDate) return null;
        const target = new Date(`${reminder.targetDate}T${time}:00`);
        if (target.getTime() <= fromDate.getTime()) {
          return null; // Expired
        }
        return {
          date: reminder.targetDate,
          time,
          isoString: target.toISOString(),
        };
      }

      case 'DAILY': {
        const candidate = new Date(fromDate);
        candidate.setHours(hours, minutes, 0, 0);

        if (candidate.getTime() <= fromDate.getTime()) {
          // Push to tomorrow
          candidate.setDate(candidate.getDate() + 1);
        }

        const dateStr = candidate.toISOString().split('T')[0];
        return {
          date: dateStr,
          time,
          isoString: candidate.toISOString(),
        };
      }

      case 'WEEKLY':
      case 'CUSTOM_WEEKDAYS': {
        const scheduledDays = reminder.scheduledDays && reminder.scheduledDays.length > 0 
          ? reminder.scheduledDays 
          : [1]; // Default Monday if missing

        // Look up to 14 days ahead
        for (let offset = 0; offset <= 14; offset++) {
          const candidate = new Date(fromDate);
          candidate.setDate(candidate.getDate() + offset);
          candidate.setHours(hours, minutes, 0, 0);

          // JS getDay(): 0 is Sun, 1 is Mon... convert to 1=Mon, 7=Sun
          const jsDay = candidate.getDay();
          const dayNumber = jsDay === 0 ? 7 : jsDay;

          if (scheduledDays.includes(dayNumber)) {
            if (candidate.getTime() > fromDate.getTime()) {
              const dateStr = candidate.toISOString().split('T')[0];
              return {
                date: dateStr,
                time,
                isoString: candidate.toISOString(),
              };
            }
          }
        }
        return null;
      }

      case 'FIXED_INTERVAL':
      case 'COMPLETION_BASED': {
        const interval = reminder.intervalMinutes || 60;
        const nextTime = new Date(fromDate.getTime() + interval * 60 * 1000);
        const dateStr = nextTime.toISOString().split('T')[0];
        const h = String(nextTime.getHours()).padStart(2, '0');
        const m = String(nextTime.getMinutes()).padStart(2, '0');
        return {
          date: dateStr,
          time: `${h}:${m}`,
          isoString: nextTime.toISOString(),
        };
      }

      default:
        return null;
    }
  }

  /**
   * Generates the next N occurrences (e.g. 5 occurrences for UI preview).
   */
  static getNextOccurrences(reminder: Reminder, count: number = 5, fromDate: Date = new Date()): NextOccurrenceResult[] {
    const results: NextOccurrenceResult[] = [];
    let currentFrom = new Date(fromDate.getTime());

    for (let i = 0; i < count; i++) {
      const next = this.getNextOccurrence(reminder, currentFrom);
      if (!next) break;
      results.push(next);
      // Advance currentFrom to 1 minute after this occurrence
      currentFrom = new Date(new Date(next.isoString).getTime() + 60 * 1000);
    }

    return results;
  }
}
