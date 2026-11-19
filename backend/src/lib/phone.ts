const bnDigits = '০১২৩৪৫৬৭৮৯';

/** Normalises Bangladeshi mobile numbers to +8801XXXXXXXXX, or returns null. */
export function normalizeBdPhone(input: unknown): string | null {
  if (typeof input !== 'string') return null;
  const ascii = [...input].map((c) => {
    const i = bnDigits.indexOf(c);
    return i >= 0 ? String(i) : c;
  }).join('');
  const digits = ascii.replace(/\D/g, '');
  let local: string;
  if (digits.startsWith('8801') && digits.length === 13) local = digits.slice(2);
  else if (digits.startsWith('01') && digits.length === 11) local = digits;
  else if (digits.startsWith('1') && digits.length === 10) local = '0' + digits;
  else return null;
  if (!/^01[3-9]\d{8}$/.test(local)) return null;
  return '+88' + local;
}

/** Masks a phone for logs: +88017•••••678 */
export function maskPhone(phone: string): string {
  return phone.length > 8 ? phone.slice(0, 6) + '•••••' + phone.slice(-3) : '•••';
}
