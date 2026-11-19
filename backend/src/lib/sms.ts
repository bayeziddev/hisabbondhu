import type { Env } from '../env';

export class SmsError extends Error {}

/**
 * Sends the OTP SMS. Providers are pluggable through the SMS_PROVIDER variable so you can
 * switch gateways without code changes. Bangladeshi gateways: BulkSMSBD, SSL Wireless, or any
 * HTTP relay ("webhook").
 */
export async function sendOtpSms(env: Env, phone: string, code: string): Promise<void> {
  const text = `HisabBondhu code: ${code}. Valid for 5 minutes. Never share this code. হিসাববন্ধু কোড: ${code}`;
  const provider = (env.SMS_PROVIDER || 'log').toLowerCase();
  const msisdn = phone.replace('+', ''); // 8801XXXXXXXXX

  if (provider === 'log') {
    // Development only: nothing is sent.
    console.log(`[sms:log] would send OTP to ${phone.slice(0, 6)}…`);
    return;
  }
  if (!env.SMS_API_KEY) throw new SmsError('SMS_API_KEY is not configured');

  let res: Response;
  if (provider === 'bulksmsbd') {
    const url = new URL('https://bulksmsbd.net/api/smsapi');
    url.searchParams.set('api_key', env.SMS_API_KEY);
    url.searchParams.set('type', 'unicode');
    url.searchParams.set('number', msisdn);
    url.searchParams.set('senderid', env.SMS_SENDER_ID || '');
    url.searchParams.set('message', text);
    res = await fetch(url.toString(), { method: 'GET' });
    const body = (await res.json().catch(() => ({}))) as { response_code?: number };
    if (!res.ok || body.response_code !== 202) throw new SmsError(`bulksmsbd failed: ${res.status} ${body.response_code}`);
    return;
  }
  if (provider === 'sslwireless') {
    res = await fetch('https://smsplus.sslwireless.com/api/v3/send-sms', {
      method: 'POST',
      headers: { 'content-type': 'application/json', accept: 'application/json' },
      body: JSON.stringify({
        api_token: env.SMS_API_KEY,
        sid: env.SMS_SENDER_ID,
        msisdn,
        sms: text,
        csms_id: crypto.randomUUID().replace(/-/g, '').slice(0, 20),
      }),
    });
    const body = (await res.json().catch(() => ({}))) as { status?: string };
    if (!res.ok || String(body.status).toUpperCase() !== 'SUCCESS') throw new SmsError(`sslwireless failed: ${res.status}`);
    return;
  }
  if (provider === 'webhook') {
    if (!env.SMS_API_URL) throw new SmsError('SMS_API_URL is not configured');
    res = await fetch(env.SMS_API_URL, {
      method: 'POST',
      headers: { 'content-type': 'application/json', authorization: `Bearer ${env.SMS_API_KEY}` },
      body: JSON.stringify({ to: phone, message: text }),
    });
    if (!res.ok) throw new SmsError(`webhook failed: ${res.status}`);
    return;
  }
  throw new SmsError(`Unknown SMS_PROVIDER "${provider}"`);
}
