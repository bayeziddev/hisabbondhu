export type UserRow = {
  id: string;
  email: string;
  phone: string | null;
  name: string | null;
  phone_verified_at: number | null;
  marketing_opt_in: number;
  pro_until: number | null;
};

export const USER_COLUMNS = 'id, email, phone, name, phone_verified_at, marketing_opt_in, pro_until';

/** Shape returned to the app. profile_complete = name + mobile number are filled in. */
export function publicUser(u: UserRow) {
  return {
    id: u.id,
    email: u.email,
    phone: u.phone,
    name: u.name,
    phone_verified: !!u.phone_verified_at,
    marketing_opt_in: !!u.marketing_opt_in,
    profile_complete: !!(u.name && u.phone),
    pro_until: u.pro_until ?? null,
    pro_active: !!u.pro_until && u.pro_until > Date.now(),
  };
}

export async function getUser(db: D1Database, id: string): Promise<UserRow | null> {
  return db.prepare(`SELECT ${USER_COLUMNS} FROM users WHERE id = ?`).bind(id).first<UserRow>();
}
