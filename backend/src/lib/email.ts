import type { Env } from '../env';

export class EmailError extends Error {}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Lower-cases and validates an email address. Returns null when invalid. */
export function normalizeEmail(input: unknown): string | null {
  if (typeof input !== 'string') return null;
  const e = input.trim().toLowerCase();
  if (e.length > 254 || !EMAIL_RE.test(e)) return null;
  return e;
}

export function maskEmail(email: string): string {
  const [u, d] = email.split('@');
  return (u.length <= 2 ? u[0] + '•' : u.slice(0, 2) + '•••') + '@' + (d ?? '');
}

/** Sends the login code with Resend (https://resend.com). EMAIL_PROVIDER=log sends nothing (development). */
export async function sendOtpEmail(env: Env, to: string, code: string): Promise<void> {
  const provider = (env.EMAIL_PROVIDER || 'resend').toLowerCase();
  if (provider === 'log') {
    console.log(`[email:log] would send OTP to ${maskEmail(to)}`);
    return;
  }
  if (provider !== 'resend') throw new EmailError(`Unknown EMAIL_PROVIDER "${provider}"`);
  if (!env.RESEND_API_KEY) throw new EmailError('RESEND_API_KEY is not configured');

  const subject = `${code} — হিসাববন্ধু লগইন কোড · HisabBondhu login code`;
  const text =
    `আপনার হিসাববন্ধু লগইন কোড: ${code}\nকোডটি ৫ মিনিট কার্যকর। কাউকে এই কোড দেবেন না।\n\n` +
    `Your HisabBondhu login code: ${code}\nIt expires in 5 minutes. Never share this code.\n\n` +
    `আপনি অনুরোধ না করলে এই ইমেইল উপেক্ষা করুন। / If you didn't request this, ignore this email.`;
  const html = `<!doctype html><html><body style="margin:0;background:#F8F9FA;font-family:'Hind Siliguri','Noto Sans Bengali',Arial,sans-serif;color:#212529">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:24px 12px">
<table role="presentation" width="100%" style="max-width:480px;background:#ffffff;border:1px solid #DEE2E6;border-radius:16px" cellpadding="0" cellspacing="0">
<tr><td style="background:#087F5B;border-radius:16px 16px 0 0;padding:18px 24px;color:#ffffff;font-size:22px;font-weight:700">হিসাববন্ধু <span style="font-size:14px;font-weight:400;opacity:.85">HisabBondhu</span></td></tr>
<tr><td style="padding:24px">
<p style="margin:0 0 8px;font-size:16px">আপনার লগইন কোড · Your login code</p>
<p style="margin:0 0 16px;font-size:36px;font-weight:700;letter-spacing:8px;color:#087F5B">${code}</p>
<p style="margin:0 0 6px;font-size:14px">কোডটি ৫ মিনিট কার্যকর। কাউকে এই কোড দেবেন না।</p>
<p style="margin:0 0 16px;font-size:14px;color:#495057">It expires in 5 minutes. Never share this code with anyone.</p>
<p style="margin:0;font-size:12px;color:#6C757D">আপনি অনুরোধ না করলে এই ইমেইল উপেক্ষা করুন। If you didn't request this, you can ignore this email.</p>
</td></tr></table>
<p style="font-size:12px;color:#6C757D;margin:12px 0 0">দোকানের হিসাব, সবসময় হাতের কাছে</p>
</td></tr></table></body></html>`;

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: { authorization: `Bearer ${env.RESEND_API_KEY}`, 'content-type': 'application/json' },
    body: JSON.stringify({
      from: env.EMAIL_FROM || 'HisabBondhu <info@sayadbayezid.com>',
      to: [to],
      subject,
      text,
      html,
      ...(env.SUPPORT_EMAIL ? { reply_to: env.SUPPORT_EMAIL } : {}),
    }),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new EmailError(`resend failed: ${res.status} ${body.slice(0, 200)}`);
  }
}
