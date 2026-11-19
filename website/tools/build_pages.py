#!/usr/bin/env python3
"""Builds the static website pages (bilingual). Edit content here, then run:
    python3 website/tools/build_pages.py
Pages: index, privacy, terms, your-rights, data-deletion, contact, 404."""
import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
EFFECTIVE_BN = "২০ নভেম্বর ২০২৬"
EFFECTIVE_EN = "20 November 2026"

HEAD = """<!doctype html>
<html lang="bn" data-lang="bn">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<meta name="description" content="{desc}">
<meta name="theme-color" content="#087F5B">
<link rel="icon" href="assets/logo.svg" type="image/svg+xml">
<meta property="og:title" content="{title}">
<meta property="og:description" content="{desc}">
<meta property="og:image" content="img/feature-graphic.png">
<script>
(function(){{var r=document.documentElement,ua=navigator.userAgent||'',m=ua.match(/HisabBondhuApp lang\\/(bn|en)/);
if(m){{r.classList.add('in-app');r.setAttribute('data-lang',m[1]);return;}}
var l=null;try{{l=localStorage.getItem('hb-lang')}}catch(e){{}}
if(!l){{l=(navigator.language||'bn').toLowerCase().indexOf('bn')===0?'bn':((navigator.language||'').toLowerCase().indexOf('en')===0?'en':'bn')}}
r.setAttribute('data-lang',l);}})();
</script>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Hind+Siliguri:wght@400;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="assets/site.css">
<script src="assets/config.js"></script>
<script src="assets/site.js" defer></script>
</head>
<body>
<header class="site-header"><div class="wrap">
  <a class="brand" href="index.html"><img src="assets/logo.svg" alt=""><span>হিসাববন্ধু</span></a>
  <nav class="nav" aria-label="Main">
    <a href="index.html"{a_home}><span class="i18n" lang="bn">হোম</span><span class="i18n" lang="en">Home</span></a>
    <a href="privacy.html"{a_privacy}><span class="i18n" lang="bn">গোপনীয়তা</span><span class="i18n" lang="en">Privacy</span></a>
    <a href="terms.html"{a_terms}><span class="i18n" lang="bn">শর্তাবলী</span><span class="i18n" lang="en">Terms</span></a>
    <a href="your-rights.html"{a_rights}><span class="i18n" lang="bn">আপনার অধিকার</span><span class="i18n" lang="en">Your rights</span></a>
    <a href="data-deletion.html"{a_delete}><span class="i18n" lang="bn">ডেটা মুছুন</span><span class="i18n" lang="en">Delete data</span></a>
    <a href="contact.html"{a_contact}><span class="i18n" lang="bn">যোগাযোগ</span><span class="i18n" lang="en">Contact</span></a>
  </nav>
</div></header>
<main class="wrap">
<div class="lang-switch" role="group" aria-label="Language"><button type="button" data-lang="bn" aria-pressed="true">বাংলা</button><button type="button" data-lang="en" aria-pressed="false">English</button></div>
"""

FOOT = """
</main>
<footer class="site-footer"><div class="wrap">
  <span>© <span id="year">2026</span> HisabBondhu · হিসাববন্ধু</span>
  <a href="privacy.html">Privacy Policy</a><a href="terms.html">Terms</a><a href="your-rights.html">Your rights</a>
  <a href="data-deletion.html">Delete account &amp; data</a><a href="contact.html">Contact</a>
  <a data-email href="mailto:support@hisabbondhu.com">support@hisabbondhu.com</a>
</div></footer>
</body>
</html>
"""

def page(fname, title, desc, active, body):
    keys = {k: "" for k in ["a_home", "a_privacy", "a_terms", "a_rights", "a_delete", "a_contact"]}
    keys["a_" + active] = ' aria-current="page"'
    html = HEAD.format(title=title, desc=desc, **keys) + body + FOOT
    with open(os.path.join(OUT, fname), "w", encoding="utf-8") as f:
        f.write(html)

def bi(bn, en):
    return f'<div class="i18n" lang="bn">{bn}</div>\n<div class="i18n" lang="en">{en}</div>'

EMAIL = '<a data-email href="mailto:support@hisabbondhu.com">support@hisabbondhu.com</a>'

# ---------------------------------------------------------------- index
page("index.html", "হিসাববন্ধু — দোকানের হিসাব, সবসময় হাতের কাছে | HisabBondhu",
     "Bangla offline-first digital ledger (khata) for small shops in Bangladesh: sales, dues, cash, expenses and stock.",
     "home", """
<section class="doc" style="text-align:center">
  <img src="assets/logo.svg" alt="HisabBondhu logo" width="96" height="96" style="border-radius:22px">
""" + bi("""
  <h1>হিসাববন্ধু</h1>
  <p style="font-size:21px;margin:4px 0 12px"><b>দোকানের হিসাব, সবসময় হাতের কাছে</b></p>
  <p>বাংলাদেশের ছোট ব্যবসার জন্য সহজ বাংলা ডিজিটাল খাতা। বিক্রি, বাকি, টাকা পাওয়া, খরচ, কেনা ও স্টক — সব এক জায়গায়।
  <b>ইন্টারনেট ছাড়াই চলে</b>, নেট এলে নিজে থেকেই নিরাপদে ব্যাকআপ হয়।</p>
  <p><a class="btn" data-play href="#">Google Play থেকে ডাউনলোড করুন</a></p>""", """
  <h1>HisabBondhu</h1>
  <p style="font-size:21px;margin:4px 0 12px"><b>Your shop's accounts, always at hand</b></p>
  <p>A simple Bangla digital ledger (khata) for small businesses in Bangladesh. Sales, dues, payments, expenses, purchases and stock —
  all in one place. <b>Works without internet</b> and backs up automatically when you are online.</p>
  <p><a class="btn" data-play href="#">Get it on Google Play</a></p>""") + """
  <img src="img/feature-graphic.png" alt="HisabBondhu app preview" style="border-radius:16px;margin-top:12px">
</section>
<section class="doc">
""" + bi("""
  <h2>কেন হিসাববন্ধু?</h2>
  <ul>
    <li><b>অফলাইন-ফার্স্ট:</b> ইন্টারনেট না থাকলেও বিক্রি, বাকি, খরচ সব লিখুন। হিসাব আগে ফোনে সংরক্ষিত হয়।</li>
    <li><b>নিজে থেকে sync:</b> লগইন করলে নেট এলেই ব্যাকআপ — অ্যাপ বন্ধ থাকলেও। ফোন হারালেও হিসাব হারাবে না।</li>
    <li><b>বাকির খাতা:</b> প্রতিটি কাস্টমার ও সাপ্লায়ারের পাবো/দেবো, রিমাইন্ডার, WhatsApp/SMS-এ বাংলা মেসেজ, PDF স্টেটমেন্ট।</li>
    <li><b>স্টক:</b> বিক্রি ও কেনায় স্টক নিজে থেকে কমে-বাড়ে, কম স্টকের সতর্কতা।</li>
    <li><b>রিপোর্ট:</b> আজকের বিক্রি, নগদ আদায়, খরচ, আনুমানিক লাভ; Excel (CSV) ও সম্পূর্ণ ব্যাকআপ ফাইল।</li>
    <li><b>নির্ভুল হিসাব:</b> টাকা পয়সায় হিসাব হয়, ভুল হলে মুছে না ফেলে বাতিল (উল্টো) হিসাব — ইতিহাস সবসময় বিশ্বাসযোগ্য।</li>
    <li><b>গোপনীয়তা:</b> কোনো বিজ্ঞাপন নেই, আপনার তথ্য বিক্রি করা হয় না। যেকোনো সময় ডেটা এক্সপোর্ট বা মুছে ফেলতে পারবেন।</li>
  </ul>""", """
  <h2>Why HisabBondhu?</h2>
  <ul>
    <li><b>Offline-first:</b> record sales, dues and expenses with no internet. Every entry is saved on the phone first.</li>
    <li><b>Automatic sync:</b> once you log in, data backs up whenever you're online — even if the app is closed.</li>
    <li><b>Credit ledger:</b> what each customer and supplier owes, reminders, Bangla WhatsApp/SMS messages, PDF statements.</li>
    <li><b>Stock:</b> stock goes down with sales and up with purchases automatically, with low-stock alerts.</li>
    <li><b>Reports:</b> today's sales, cash collected, expenses, estimated profit; Excel (CSV) export and full backup file.</li>
    <li><b>Accurate:</b> money is stored in paisa; mistakes are cancelled with a reversal entry, never silently deleted.</li>
    <li><b>Private:</b> no ads, we never sell your data, and you can export or delete your data at any time.</li>
  </ul>""") + """
</section>
""")

# ---------------------------------------------------------------- privacy
PRIV_BN = f"""
<h1>গোপনীয়তা নীতি</h1>
<div class="meta">কার্যকর তারিখ: {EFFECTIVE_BN}</div>
<p>এই নীতিতে বলা হয়েছে <b>হিসাববন্ধু</b> (“আমরা”) Android অ্যাপ, ওয়েবসাইট ও সার্ভার (একত্রে “সেবা”) কী তথ্য সংগ্রহ করে, কেন করে, কীভাবে সুরক্ষিত রাখে এবং আপনার কী কী অধিকার আছে।
প্রশ্ন থাকলে যোগাযোগ করুন: {EMAIL}</p>

<div class="note"><b>সংক্ষেপে:</b> লগইন না করলে আপনার সব হিসাব শুধু আপনার ফোনেই থাকে — আমরা দেখতে পাই না।
লগইন করলে ব্যাকআপ ও sync-এর জন্য হিসাব আমাদের সার্ভারে নিরাপদে রাখা হয়। আমরা বিজ্ঞাপন দেখাই না, তথ্য বিক্রি করি না, কোনো অ্যানালিটিক্স বা ট্র্যাকিং SDK ব্যবহার করি না।</div>

<h2>১. আমরা কোন তথ্য সংগ্রহ করি</h2>
<table>
<tr><th>তথ্য</th><th>কখন</th><th>কেন</th></tr>
<tr><td>দোকানের তথ্য (নাম, মালিকের নাম, মোবাইল, ঠিকানা, ব্যবসার ধরন, শুরুর ক্যাশ)</td><td>আপনি লিখলে</td><td>অ্যাপ চালানো, স্টেটমেন্ট ও মেসেজে দোকানের নাম দেখানো</td></tr>
<tr><td>আপনার কাস্টমার ও সাপ্লায়ারের নাম, মোবাইল, ঠিকানা, নোট</td><td>আপনি লিখলে বা ফোনবুক থেকে একজনকে বাছাই করলে</td><td>বাকির খাতা ও মেসেজ পাঠানো</td></tr>
<tr><td>পণ্য, দাম, স্টক এবং লেনদেন (বিক্রি, কেনা, খরচ, টাকা আদান-প্রদান)</td><td>আপনি লিখলে</td><td>হিসাব, স্টক ও রিপোর্ট তৈরি</td></tr>
<tr><td>মোবাইল নম্বর ও এককালীন কোড (OTP)</td><td>লগইন করলে</td><td>পরিচয় যাচাই। OTP শুধু হ্যাশ করে রাখা হয় এবং ২৪ ঘণ্টার মধ্যে মুছে যায়</td></tr>
<tr><td>ডিভাইসের আইডি (অ্যাপের তৈরি র‍্যান্ডম আইডি) ও ডিভাইসের মডেলের নাম</td><td>লগইন ও sync করলে</td><td>কোন ফোন থেকে হিসাব এসেছে বোঝা, লগইন করা ডিভাইস দেখানো, ডুপ্লিকেট রোধ</td></tr>
<tr><td>IP ঠিকানা ও প্রযুক্তিগত লগ</td><td>সার্ভারে অনুরোধ এলে</td><td>নিরাপত্তা, অপব্যবহার ও স্প্যাম রোধ (রেট লিমিট), সমস্যা সমাধান</td></tr>
<tr><td>আপনার পাঠানো মেসেজ (নাম, মোবাইল/ইমেইল, বিষয়, মেসেজ)</td><td>যোগাযোগ ফর্ম ব্যবহার করলে</td><td>আপনার প্রশ্নের উত্তর দেওয়া</td></tr>
</table>
<p><b>যা আমরা সংগ্রহ করি না:</b> লোকেশন, আপনার পুরো ফোনবুক, SMS পড়া, ছবি/ফাইল, বিজ্ঞাপন আইডি (Advertising ID), বা কোনো ব্যাংক/মোবাইল ব্যাংকিং অ্যাকাউন্টের তথ্য।
ফোনবুক থেকে কাস্টমার যোগ করলে Android-এর সিস্টেম পিকার শুধু আপনার বাছাই করা একটি নম্বর অ্যাপকে দেয় — অ্যাপের ফোনবুক পড়ার অনুমতি নেই।</p>

<h2>২. তথ্য কোথায় থাকে</h2>
<ul>
<li><b>লগইন ছাড়া:</b> সব তথ্য শুধু আপনার ফোনের অ্যাপের প্রাইভেট স্টোরেজে থাকে। Android-এর ক্লাউড ব্যাকআপ থেকেও এই ডেটা বাদ রাখা হয়েছে।</li>
<li><b>লগইন করলে:</b> তথ্য HTTPS (এনক্রিপ্টেড সংযোগ) দিয়ে Cloudflare-এর সার্ভারে (Workers, D1 ডেটাবেস, R2 স্টোরেজ) সংরক্ষিত হয়, যেখানে ডেটা স্টোরেজেও এনক্রিপ্টেড থাকে। Cloudflare-এর নেটওয়ার্ক বিশ্বব্যাপী, তাই তথ্য বাংলাদেশের বাইরের ডেটা সেন্টারেও প্রক্রিয়া হতে পারে।</li>
</ul>

<h2>৩. তথ্য কার সাথে শেয়ার করা হয়</h2>
<p>আমরা কারো কাছে আপনার তথ্য <b>বিক্রি করি না</b> এবং বিজ্ঞাপনের জন্য ব্যবহার করি না। শুধু সেবা চালাতে প্রয়োজনীয় এই প্রতিষ্ঠানগুলো সীমিত তথ্য প্রক্রিয়া করে:</p>
<ul>
<li><b>Cloudflare, Inc.</b> — সার্ভার, ডেটাবেস ও ফাইল স্টোরেজ।</li>
<li><b>SMS গেটওয়ে প্রদানকারী (বাংলাদেশ)</b> — শুধু আপনার মোবাইল নম্বর ও OTP মেসেজ, কোড পাঠানোর জন্য।</li>
<li><b>Google Play</b> — অ্যাপ বিতরণ ও আপডেট (Google-এর নিজস্ব নীতি প্রযোজ্য)।</li>
</ul>
<p>আইন অনুযায়ী বাধ্য হলে (যেমন আদালতের বৈধ আদেশ) বা প্রতারণা/নিরাপত্তা ঝুঁকি ঠেকাতে প্রয়োজনীয় সর্বনিম্ন তথ্য প্রকাশ করা হতে পারে।
আপনি নিজে “শেয়ার” চাপলে মেসেজ বা PDF আপনার বাছাই করা অ্যাপে (WhatsApp, SMS ইত্যাদি) যায় — সেটি আপনার নিয়ন্ত্রণে।</p>

<h2>৪. আপনার কাস্টমারদের তথ্য</h2>
<p>আপনি যে কাস্টমার/সাপ্লায়ারের তথ্য লেখেন, সেই তথ্যের দায়িত্ব আপনার; আমরা আপনার পক্ষ থেকে শুধু সংরক্ষণ ও প্রক্রিয়া করি। অনুগ্রহ করে তাদের অনুমতি নিয়ে তথ্য রাখুন ও শুধু প্রয়োজনীয় মেসেজ পাঠান — স্প্যাম করবেন না।</p>

<h2>৫. নিরাপত্তা</h2>
<ul>
<li>সব সংযোগ HTTPS/TLS; এনক্রিপ্ট-না-করা (cleartext) সংযোগ অ্যাপে বন্ধ।</li>
<li>OTP ও লগইন টোকেন কখনো সরাসরি রাখা হয় না — শুধু হ্যাশ রাখা হয়; লগইন টোকেন নিয়মিত বদলায় (rotation)।</li>
<li>প্রতিটি ব্যবসার তথ্য আলাদা রাখা হয় (tenant isolation) — অন্য কেউ আপনার হিসাব দেখতে পারে না।</li>
<li>OTP অনুরোধ ও ভুল চেষ্টার সীমা, অপব্যবহার রোধে রেট লিমিট।</li>
<li>লেনদেন মুছে ফেলা যায় না, শুধু বাতিল হিসাব যোগ হয় — হিসাবের অডিট ট্রেইল থাকে।</li>
</ul>
<p>কোনো ব্যবস্থাই ১০০% নিরাপদ নয়; নিরাপত্তা ঘটনা ঘটলে আমরা যুক্তিসঙ্গত সময়ের মধ্যে আক্রান্ত ব্যবহারকারীদের জানাবো।</p>

<h2>৬. কতদিন রাখা হয়</h2>
<table>
<tr><th>তথ্য</th><th>সময়</th></tr>
<tr><td>ব্যবসার হিসাব (লগইন করা অবস্থায়)</td><td>যতদিন অ্যাকাউন্ট থাকে, বা আপনি মুছে ফেলা পর্যন্ত</td></tr>
<tr><td>OTP কোড (হ্যাশ)</td><td>সর্বোচ্চ ২৪ ঘণ্টা</td></tr>
<tr><td>লগইন সেশন</td><td>শেষ ব্যবহারের পর ৬০ দিন; লগআউট বা বাতিলের ৩০ দিন পর মুছে যায়</td></tr>
<tr><td>নিরাপত্তা/অডিট লগ</td><td>১২ মাস</td></tr>
<tr><td>সাপ্তাহিক দুর্যোগ-পুনরুদ্ধার কপি</td><td>সর্বোচ্চ ৮ সপ্তাহ; অ্যাকাউন্ট মুছলে সঙ্গে সঙ্গে মুছে ফেলা হয়</td></tr>
<tr><td>ডেটাবেসের স্বয়ংক্রিয় পয়েন্ট-ইন-টাইম ব্যাকআপ (Cloudflare D1)</td><td>৩০ দিন পর নিজে থেকে মুছে যায়</td></tr>
<tr><td>যোগাযোগ ফর্মের মেসেজ</td><td>সর্বোচ্চ ২ বছর</td></tr>
<tr><td>মুছে ফেলার রেকর্ড</td><td>শুধু ফোন নম্বরের একমুখী হ্যাশ ও তারিখ — প্রমাণ হিসেবে</td></tr>
</table>

<h2>৭. ডেটা মুছে ফেলা</h2>
<p>অ্যাপে: <b>আরও → অ্যাকাউন্ট ও ডেটা মুছুন</b>। অ্যাপ ছাড়া: <a href="data-deletion.html">ডেটা মুছে ফেলার পাতা</a> থেকে মোবাইল নম্বর যাচাই করে মুছুন। সার্ভারের তথ্য সঙ্গে সঙ্গে মুছে যায়; স্বয়ংক্রিয় ব্যাকআপ কপি ৩০ দিনের মধ্যে মুছে যায়।</p>

<h2>৮. আপনার অধিকার</h2>
<p>তথ্য দেখা, সংশোধন, এক্সপোর্ট (CSV/JSON), মুছে ফেলা ও সম্মতি প্রত্যাহারের অধিকার আছে। বিস্তারিত: <a href="your-rights.html">আপনার অধিকার</a>।</p>

<h2>৯. শিশুদের গোপনীয়তা</h2>
<p>এই অ্যাপ ব্যবসা পরিচালনাকারী প্রাপ্তবয়স্কদের জন্য। আমরা জেনেশুনে ১৮ বছরের কম বয়সী কারো তথ্য সংগ্রহ করি না। এমন তথ্য পেলে মুছে ফেলবো।</p>

<h2>১০. পরিবর্তন</h2>
<p>নীতিতে বড় পরিবর্তন হলে অ্যাপ বা ওয়েবসাইটে জানানো হবে এবং কার্যকর তারিখ হালনাগাদ করা হবে।</p>

<h2>১১. যোগাযোগ</h2>
<p>হিসাববন্ধু, ঢাকা, বাংলাদেশ · ইমেইল: {EMAIL} · <a href="contact.html">যোগাযোগ ফর্ম</a></p>
"""

PRIV_EN = f"""
<h1>Privacy Policy</h1>
<div class="meta">Effective date: {EFFECTIVE_EN}</div>
<p>This policy explains what information <b>HisabBondhu</b> (“we”, “us”) collects through its Android app, website and servers (together, the “Service”),
why we collect it, how we protect it, and the rights you have. Questions: {EMAIL}</p>

<div class="note"><b>In short:</b> if you don't log in, all your records stay only on your phone — we cannot see them.
If you log in, your records are stored securely on our servers for backup and sync. We show no ads, never sell data, and use no analytics or tracking SDKs.</div>

<h2>1. Information we collect</h2>
<table>
<tr><th>Information</th><th>When</th><th>Why</th></tr>
<tr><td>Business details (shop name, owner name, mobile, address, business type, opening cash)</td><td>When you enter them</td><td>To run the app and show your shop name on statements and messages</td></tr>
<tr><td>Names, mobile numbers, addresses and notes of your customers and suppliers</td><td>When you type them or pick one contact</td><td>Credit ledger and messages you choose to send</td></tr>
<tr><td>Products, prices, stock and transactions (sales, purchases, expenses, payments)</td><td>When you enter them</td><td>Accounting, stock and reports</td></tr>
<tr><td>Mobile number and one-time code (OTP)</td><td>When you log in</td><td>Verifying your identity. OTPs are stored only as hashes and deleted within 24 hours</td></tr>
<tr><td>Device ID (a random ID created by the app) and device model name</td><td>When you log in and sync</td><td>Knowing which phone sent an entry, listing logged-in devices, preventing duplicates</td></tr>
<tr><td>IP address and technical logs</td><td>When the server receives a request</td><td>Security, abuse and spam prevention (rate limiting), troubleshooting</td></tr>
<tr><td>Messages you send us (name, mobile/email, subject, message)</td><td>When you use the contact form</td><td>Replying to you</td></tr>
</table>
<p><b>We do not collect:</b> location, your full contact list, SMS contents, photos/files, the Advertising ID, or any bank/mobile-wallet account credentials.
When you add a customer from contacts, Android's system picker gives the app only the single number you choose — the app has no permission to read your contacts.</p>

<h2>2. Where your data is stored</h2>
<ul>
<li><b>Without login:</b> everything stays in the app's private storage on your phone. This data is also excluded from Android cloud backup.</li>
<li><b>With login:</b> data is sent over HTTPS and stored on Cloudflare infrastructure (Workers, D1 database, R2 storage), encrypted at rest. Cloudflare operates a global network, so data may be processed in data centres outside Bangladesh.</li>
</ul>

<h2>3. Sharing</h2>
<p>We <b>do not sell</b> your information or use it for advertising. Only these providers process limited data to run the Service:</p>
<ul>
<li><b>Cloudflare, Inc.</b> — hosting, database and file storage.</li>
<li><b>SMS gateway provider (Bangladesh)</b> — only your mobile number and the OTP message, to deliver the code.</li>
<li><b>Google Play</b> — app distribution and updates (Google's own policy applies).</li>
</ul>
<p>We may disclose the minimum necessary information if required by law (e.g. a valid court order) or to prevent fraud or a security threat.
When you tap “Share”, the message or PDF goes to the app you choose (WhatsApp, SMS, etc.) — that is under your control.</p>

<h2>4. Your customers' information</h2>
<p>You are responsible for the customer/supplier information you enter; we store and process it on your behalf.
Please keep it with their permission and send only necessary messages — no spam.</p>

<h2>5. Security</h2>
<ul>
<li>All connections use HTTPS/TLS; unencrypted (cleartext) traffic is blocked in the app.</li>
<li>OTPs and login tokens are never stored in plain form — only hashes; refresh tokens rotate on every use.</li>
<li>Each business's data is isolated (tenant isolation) — nobody else can access your ledger.</li>
<li>Limits on OTP requests and wrong attempts; rate limiting against abuse.</li>
<li>Transactions cannot be deleted, only reversed — keeping an audit trail of your accounts.</li>
</ul>
<p>No system is 100% secure; if a security incident affects you, we will notify you within a reasonable time.</p>

<h2>6. Retention</h2>
<table>
<tr><th>Data</th><th>Kept for</th></tr>
<tr><td>Business records (logged-in users)</td><td>As long as the account exists, or until you delete them</td></tr>
<tr><td>OTP codes (hashed)</td><td>Up to 24 hours</td></tr>
<tr><td>Login sessions</td><td>60 days after last use; removed 30 days after logout/revocation</td></tr>
<tr><td>Security/audit logs</td><td>12 months</td></tr>
<tr><td>Weekly disaster-recovery snapshots</td><td>Up to 8 weeks; deleted immediately when the account is deleted</td></tr>
<tr><td>Automatic point-in-time database backups (Cloudflare D1)</td><td>Expire automatically after 30 days</td></tr>
<tr><td>Contact form messages</td><td>Up to 2 years</td></tr>
<tr><td>Deletion records</td><td>Only a one-way hash of the phone number and the date — as proof</td></tr>
</table>

<h2>7. Deleting your data</h2>
<p>In the app: <b>More → Delete account &amp; data</b>. Without the app: use the <a href="data-deletion.html">data deletion page</a> and verify your mobile number.
Server data is deleted immediately; automatic backup copies expire within 30 days.</p>

<h2>8. Your rights</h2>
<p>You can access, correct, export (CSV/JSON), and delete your data, and withdraw consent. Details: <a href="your-rights.html">Your rights</a>.</p>

<h2>9. Children</h2>
<p>The app is intended for adults running a business. We do not knowingly collect information from anyone under 18 and will delete it if we learn of it.</p>

<h2>10. Changes</h2>
<p>If we make material changes, we will announce them in the app or on this website and update the effective date.</p>

<h2>11. Contact</h2>
<p>HisabBondhu, Dhaka, Bangladesh · Email: {EMAIL} · <a href="contact.html">Contact form</a></p>
"""
page("privacy.html", "গোপনীয়তা নীতি · Privacy Policy — হিসাববন্ধু", "HisabBondhu privacy policy", "privacy",
     f'<article class="doc">{bi(PRIV_BN, PRIV_EN)}</article>')

# ---------------------------------------------------------------- terms
TERMS_BN = f"""
<h1>শর্তাবলী</h1>
<div class="meta">কার্যকর তারিখ: {EFFECTIVE_BN}</div>
<p>হিসাববন্ধু অ্যাপ বা ওয়েবসাইট ব্যবহার করলে আপনি এই শর্তাবলী মেনে নিচ্ছেন। একমত না হলে অনুগ্রহ করে সেবা ব্যবহার করবেন না।</p>
<h2>১. সেবা</h2>
<p>হিসাববন্ধু ছোট ব্যবসার বিক্রি, বাকি, খরচ, কেনা ও স্টক লিখে রাখার একটি ডিজিটাল খাতা। মূল হিসাব-সুবিধা ফ্রি। ভবিষ্যতে কোনো পেইড সুবিধা এলে দাম ও শর্ত আগে পরিষ্কারভাবে জানানো হবে — কোনো গোপন চার্জ থাকবে না।</p>
<h2>২. যোগ্যতা ও অ্যাকাউন্ট</h2>
<ul>
<li>সেবা ব্যবহারে আপনার বয়স কমপক্ষে ১৮ বছর বা আইনগতভাবে চুক্তি করার যোগ্যতা থাকতে হবে।</li>
<li>লগইন ঐচ্ছিক; লগইন করলে মোবাইল নম্বর ও OTP দিয়ে যাচাই হবে। আপনার ফোন ও OTP গোপন রাখার দায়িত্ব আপনার।</li>
<li>অ্যাকাউন্টে সন্দেহজনক কিছু দেখলে সঙ্গে সঙ্গে আমাদের জানান।</li>
</ul>
<h2>৩. আপনার তথ্য, আপনার মালিকানা</h2>
<p>আপনার লেখা সব হিসাবের মালিক আপনি। সেবা চালানোর (সংরক্ষণ, sync, ব্যাকআপ, এক্সপোর্ট) জন্য যতটুকু দরকার, শুধু ততটুকু ব্যবহারের অনুমতি আপনি আমাদের দিচ্ছেন। যেকোনো সময় এক্সপোর্ট বা মুছে ফেলতে পারবেন।</p>
<h2>৪. আপনার দায়িত্ব</h2>
<ul>
<li>সঠিক তথ্য লেখা এবং হিসাব নিয়মিত যাচাই করা।</li>
<li>কাস্টমার/সাপ্লায়ারের তথ্য রাখার আগে তাদের সম্মতি নেওয়া; মেসেজ ও রিমাইন্ডার ভদ্রভাবে ও প্রয়োজন অনুযায়ী পাঠানো — স্প্যাম বা হয়রানি নিষেধ।</li>
<li>বেআইনি কাজ, প্রতারণা, অন্যের অ্যাকাউন্টে প্রবেশের চেষ্টা, সার্ভারে আক্রমণ বা অপব্যবহার না করা।</li>
<li>লগইন না করলে তথ্য শুধু ফোনে থাকে — ফোন হারালে বা অ্যাপ মুছলে তথ্য হারাতে পারে। নিয়মিত ব্যাকআপ (লগইন বা এক্সপোর্ট) রাখার পরামর্শ দেওয়া হচ্ছে।</li>
</ul>
<h2>৫. আর্থিক বা আইনি পরামর্শ নয়</h2>
<p>হিসাববন্ধু একটি রেকর্ড রাখার টুল। আনুমানিক লাভসহ সব হিসাব আপনার দেওয়া তথ্যের ওপর ভিত্তি করে তৈরি। এটি কর, হিসাবরক্ষণ বা আইনি পরামর্শ নয়; গুরুত্বপূর্ণ সিদ্ধান্তের আগে যোগ্য পেশাজীবীর পরামর্শ নিন।</p>
<h2>৬. সেবার প্রাপ্যতা</h2>
<p>অফলাইন অংশ সবসময় ফোনে চলে। অনলাইন sync সর্বোচ্চ চেষ্টায় সচল রাখা হয়, তবে রক্ষণাবেক্ষণ বা নেটওয়ার্ক সমস্যায় সাময়িক বিঘ্ন হতে পারে। সেবা উন্নত করতে বৈশিষ্ট্য পরিবর্তন হতে পারে।</p>
<h2>৭. দায়বদ্ধতার সীমা</h2>
<p>আইনের অনুমোদিত সর্বোচ্চ সীমা পর্যন্ত, সেবা “যেমন আছে” ভিত্তিতে দেওয়া হয়। পরোক্ষ ক্ষতি, ব্যবসায়িক ক্ষতি বা ভুল তথ্য লেখার ফলে হওয়া ক্ষতির জন্য আমরা দায়ী থাকবো না। আমাদের ইচ্ছাকৃত অসদাচরণ বা গুরুতর অবহেলার দায় এতে সীমিত হয় না।</p>
<h2>৮. অ্যাকাউন্ট বন্ধ</h2>
<p>আপনি যেকোনো সময় অ্যাকাউন্ট ও ডেটা মুছে ফেলতে পারেন। শর্ত ভঙ্গ বা অপব্যবহারের ক্ষেত্রে আমরা অ্যাকাউন্ট স্থগিত করতে পারি; সম্ভব হলে আগে জানিয়ে ডেটা এক্সপোর্টের সুযোগ দেওয়া হবে।</p>
<h2>৯. প্রযোজ্য আইন</h2>
<p>এই শর্তাবলী বাংলাদেশের আইন অনুযায়ী পরিচালিত হবে; বিরোধ নিষ্পত্তির এখতিয়ার ঢাকার আদালতের।</p>
<h2>১০. পরিবর্তন ও যোগাযোগ</h2>
<p>শর্তাবলী পরিবর্তন হলে অ্যাপ বা ওয়েবসাইটে জানানো হবে। পরিবর্তনের পর ব্যবহার চালিয়ে গেলে নতুন শর্ত মেনে নেওয়া হয়েছে বলে ধরা হবে। যোগাযোগ: {EMAIL}</p>
"""
TERMS_EN = f"""
<h1>Terms &amp; Conditions</h1>
<div class="meta">Effective date: {EFFECTIVE_EN}</div>
<p>By using the HisabBondhu app or website you agree to these terms. If you do not agree, please do not use the Service.</p>
<h2>1. The Service</h2>
<p>HisabBondhu is a digital ledger for small businesses to record sales, dues, expenses, purchases and stock. Core bookkeeping features are free.
If paid features are introduced, prices and terms will be shown clearly in advance — no hidden charges.</p>
<h2>2. Eligibility and account</h2>
<ul>
<li>You must be at least 18 years old or otherwise legally able to enter into a contract.</li>
<li>Login is optional; when you log in we verify your mobile number with an OTP. Keep your phone and codes secure.</li>
<li>Tell us immediately if you notice suspicious activity on your account.</li>
</ul>
<h2>3. Your data is yours</h2>
<p>You own all records you enter. You grant us only the permission needed to operate the Service (storage, sync, backup, export). You can export or delete your data at any time.</p>
<h2>4. Your responsibilities</h2>
<ul>
<li>Enter accurate information and review your records regularly.</li>
<li>Obtain consent before storing customers'/suppliers' details; send messages and reminders politely and only when needed — no spam or harassment.</li>
<li>Do not use the Service for illegal activity or fraud, attempt to access other accounts, attack or abuse our servers.</li>
<li>Without login, data lives only on your phone and can be lost if the phone is lost or the app is removed. We recommend keeping a backup (log in or export).</li>
</ul>
<h2>5. Not financial or legal advice</h2>
<p>HisabBondhu is a record-keeping tool. All figures, including estimated profit, are based on the data you enter. It is not tax, accounting or legal advice; consult a qualified professional for important decisions.</p>
<h2>6. Availability</h2>
<p>The offline part always runs on your phone. Online sync is provided on a best-effort basis and may be briefly unavailable for maintenance or network issues. Features may change as we improve the Service.</p>
<h2>7. Limitation of liability</h2>
<p>To the maximum extent permitted by law, the Service is provided “as is”. We are not liable for indirect or consequential losses, lost profits, or losses caused by incorrect data entry. Nothing here limits liability for our wilful misconduct or gross negligence.</p>
<h2>8. Termination</h2>
<p>You may delete your account and data at any time. We may suspend accounts that breach these terms or abuse the Service; where possible we will give notice and a chance to export data.</p>
<h2>9. Governing law</h2>
<p>These terms are governed by the laws of Bangladesh; the courts of Dhaka have jurisdiction over disputes.</p>
<h2>10. Changes and contact</h2>
<p>We will announce changes in the app or on the website. Continuing to use the Service after changes means you accept the updated terms. Contact: {EMAIL}</p>
"""
page("terms.html", "শর্তাবলী · Terms & Conditions — হিসাববন্ধু", "HisabBondhu terms and conditions", "terms",
     f'<article class="doc">{bi(TERMS_BN, TERMS_EN)}</article>')

# ---------------------------------------------------------------- your rights
RIGHTS_BN = f"""
<h1>আপনার অধিকার</h1>
<div class="meta">কার্যকর তারিখ: {EFFECTIVE_BN}</div>
<p>আপনার তথ্যের ওপর নিয়ন্ত্রণ আপনার। নিচের অধিকারগুলো আপনি বিনামূল্যে ব্যবহার করতে পারেন।</p>
<table>
<tr><th>অধিকার</th><th>কী বোঝায়</th><th>কীভাবে ব্যবহার করবেন</th></tr>
<tr><td>জানার অধিকার</td><td>কোন তথ্য, কেন ও কতদিন রাখা হয়</td><td><a href="privacy.html">গোপনীয়তা নীতি</a> পড়ুন</td></tr>
<tr><td>দেখার অধিকার</td><td>আমাদের কাছে আপনার কী তথ্য আছে তার কপি</td><td>অ্যাপের সব হিসাব আপনার চোখের সামনে; সার্ভারের কপি চাইলে ইমেইল করুন</td></tr>
<tr><td>সংশোধনের অধিকার</td><td>ভুল তথ্য ঠিক করা</td><td>অ্যাপে নাম/তথ্য সম্পাদনা করুন; ভুল লেনদেন “হিসাব বাতিল” করে আবার লিখুন</td></tr>
<tr><td>ডেটা বহনযোগ্যতা</td><td>মেশিনে পড়ার উপযোগী ফরম্যাটে তথ্য নেওয়া</td><td>রিপোর্ট → Excel (CSV) বা সম্পূর্ণ ব্যাকআপ (JSON)</td></tr>
<tr><td>মুছে ফেলার অধিকার</td><td>অ্যাকাউন্ট ও সব তথ্য স্থায়ীভাবে মোছা</td><td>অ্যাপে আরও → অ্যাকাউন্ট ও ডেটা মুছুন, অথবা <a href="data-deletion.html">ওয়েবসাইট থেকে</a></td></tr>
<tr><td>সম্মতি প্রত্যাহার</td><td>অনলাইন ব্যাকআপ বন্ধ করা</td><td>লগআউট করুন — অনলাইন sync বন্ধ হবে</td></tr>
<tr><td>নোটিফিকেশন বন্ধ</td><td>রিমাইন্ডার না পাওয়া</td><td>আরও → বাকি আদায়ের রিমাইন্ডার বন্ধ করুন</td></tr>
<tr><td>অভিযোগের অধিকার</td><td>আমাদের কাজ নিয়ে অসন্তুষ্ট হলে</td><td>আমাদের ইমেইল করুন; প্রয়োজনে বাংলাদেশের সংশ্লিষ্ট কর্তৃপক্ষের কাছেও অভিযোগ করতে পারেন</td></tr>
</table>
<h2>সময়সীমা ও যাচাই</h2>
<p>আমরা সাধারণত ৭ কার্যদিবসের মধ্যে এবং সর্বোচ্চ ৩০ দিনের মধ্যে অনুরোধের উত্তর দিই। আপনার নিরাপত্তার জন্য অনুরোধকারী আসলেই অ্যাকাউন্টের মালিক কিনা তা নিবন্ধিত মোবাইল নম্বরে OTP দিয়ে যাচাই করা হতে পারে।</p>
<h2>আপনার কাস্টমারদের অধিকার</h2>
<p>আপনার খাতায় যাদের তথ্য আছে, তারা তথ্য মুছতে বা সংশোধন করতে চাইলে প্রথমে আপনাকে (দোকান মালিককে) অনুরোধ করবেন। আপনি অ্যাপ থেকে সংশ্লিষ্ট নাম মুছে বা সম্পাদনা করে তা পূরণ করতে পারেন। সরাসরি আমাদের কাছে এলে আমরা আপনাকে জানাবো।</p>
<p>যোগাযোগ: {EMAIL}</p>
"""
RIGHTS_EN = f"""
<h1>Your rights</h1>
<div class="meta">Effective date: {EFFECTIVE_EN}</div>
<p>You are in control of your information. You can exercise these rights free of charge.</p>
<table>
<tr><th>Right</th><th>What it means</th><th>How</th></tr>
<tr><td>Right to be informed</td><td>What we keep, why and for how long</td><td>Read the <a href="privacy.html">Privacy Policy</a></td></tr>
<tr><td>Right of access</td><td>A copy of the data we hold about you</td><td>All your records are visible in the app; email us for a copy of server data</td></tr>
<tr><td>Right to rectification</td><td>Fix incorrect data</td><td>Edit details in the app; cancel a wrong entry with “Cancel entry” and record it again</td></tr>
<tr><td>Right to data portability</td><td>Get your data in a machine-readable format</td><td>Reports → Excel (CSV) or Full backup (JSON)</td></tr>
<tr><td>Right to erasure</td><td>Permanently delete your account and data</td><td>In the app: More → Delete account &amp; data, or <a href="data-deletion.html">on the website</a></td></tr>
<tr><td>Withdraw consent</td><td>Stop online backup</td><td>Log out — online sync stops</td></tr>
<tr><td>Opt out of notifications</td><td>Stop reminders</td><td>More → turn off Due collection reminders</td></tr>
<tr><td>Right to complain</td><td>If you are unhappy with how we handle data</td><td>Email us; you may also complain to the relevant authority in Bangladesh</td></tr>
</table>
<h2>Response time and verification</h2>
<p>We usually respond within 7 working days and always within 30 days. To protect you, we may verify that the requester owns the account by sending an OTP to the registered mobile number.</p>
<h2>Rights of your customers</h2>
<p>People listed in your ledger who want their details corrected or removed should ask you (the shop owner) first; you can edit or delete them in the app. If they contact us directly, we will let you know.</p>
<p>Contact: {EMAIL}</p>
"""
page("your-rights.html", "আপনার অধিকার · Your rights — হিসাববন্ধু", "Your data rights with HisabBondhu", "rights",
     f'<article class="doc">{bi(RIGHTS_BN, RIGHTS_EN)}</article>')

# ---------------------------------------------------------------- data deletion
DEL_BN = """
<h1>অ্যাকাউন্ট ও ডেটা মুছে ফেলার অনুরোধ</h1>
<p>হিসাববন্ধু অ্যাপের অ্যাকাউন্ট ও সংশ্লিষ্ট সব তথ্য মুছে ফেলার দুটি উপায়:</p>
<h2>উপায় ১: অ্যাপ থেকে (সবচেয়ে সহজ)</h2>
<ol><li>হিসাববন্ধু অ্যাপ খুলুন</li><li><b>আরও</b> ট্যাবে যান</li><li><b>অ্যাকাউন্ট ও ডেটা মুছুন</b> চাপুন</li><li>নিশ্চিত করুন</li></ol>
<h2>উপায় ২: এই পাতা থেকে (অ্যাপ না থাকলেও)</h2>
<p>নিবন্ধিত মোবাইল নম্বর দিন, SMS-এ আসা কোড দিয়ে যাচাই করুন, তারপর মুছে ফেলা নিশ্চিত করুন।</p>
<h2>যা মুছে যাবে</h2>
<ul><li>আপনার অ্যাকাউন্ট, মোবাইল নম্বর, লগইন সেশন ও ডিভাইস তালিকা</li>
<li>সার্ভারে থাকা সব ব্যবসার তথ্য: দোকানের প্রোফাইল, কাস্টমার, সাপ্লায়ার, পণ্য, লেনদেন</li>
<li>সাপ্তাহিক ব্যাকআপ কপি ও অডিট লগ</li></ul>
<h2>যা থেকে যায় ও কতদিন</h2>
<ul><li>ডেটাবেসের স্বয়ংক্রিয় দুর্যোগ-পুনরুদ্ধার কপি <b>৩০ দিনের মধ্যে</b> নিজে থেকে মুছে যায়</li>
<li>মুছে ফেলার প্রমাণ হিসেবে শুধু মোবাইল নম্বরের একমুখী হ্যাশ ও তারিখ রাখা হয় (নম্বর নিজে নয়)</li>
<li>যারা লগইন করেননি, তাদের তথ্য শুধু তাদের ফোনে থাকে — অ্যাপ আনইনস্টল করলেই মুছে যায়</li></ul>
"""
DEL_EN = """
<h1>Request account &amp; data deletion</h1>
<p>There are two ways to delete your HisabBondhu account and all associated data:</p>
<h2>Option 1: In the app (easiest)</h2>
<ol><li>Open the HisabBondhu app</li><li>Go to the <b>More</b> tab</li><li>Tap <b>Delete account &amp; data</b></li><li>Confirm</li></ol>
<h2>Option 2: On this page (works even without the app)</h2>
<p>Enter your registered mobile number, verify with the code we send by SMS, then confirm deletion.</p>
<h2>What is deleted</h2>
<ul><li>Your account, mobile number, login sessions and device list</li>
<li>All business data on our servers: shop profile, customers, suppliers, products, transactions</li>
<li>Weekly backup snapshots and audit logs</li></ul>
<h2>What is kept, and for how long</h2>
<ul><li>Automatic database disaster-recovery copies expire on their own <b>within 30 days</b></li>
<li>As proof of deletion we keep only a one-way hash of the mobile number and the date (not the number itself)</li>
<li>If you never logged in, your data exists only on your phone and is removed when you uninstall the app</li></ul>
"""
DEL_FORM = """
<form class="card" id="del-form" onsubmit="return false">
  <div id="step-phone">
    <label for="phone"><span class="i18n" lang="bn">নিবন্ধিত মোবাইল নম্বর</span><span class="i18n" lang="en">Registered mobile number</span></label>
    <input id="phone" type="tel" inputmode="tel" placeholder="01XXXXXXXXX" autocomplete="tel" required>
    <p><button class="btn" id="send-otp" type="button"><span class="i18n" lang="bn">কোড পাঠান</span><span class="i18n" lang="en">Send code</span></button></p>
  </div>
  <div id="step-otp" class="hidden">
    <label for="otp"><span class="i18n" lang="bn">SMS-এ আসা ৬ সংখ্যার কোড</span><span class="i18n" lang="en">6-digit code from SMS</span></label>
    <input id="otp" inputmode="numeric" maxlength="6" autocomplete="one-time-code" required>
    <p><label style="font-weight:400"><input type="checkbox" id="confirm" style="width:auto"> <span class="i18n" lang="bn">আমি বুঝেছি, এটি ফেরত আনা যাবে না</span><span class="i18n" lang="en">I understand this cannot be undone</span></label></p>
    <p><button class="btn danger" id="do-delete" type="button" disabled><span class="i18n" lang="bn">স্থায়ীভাবে মুছুন</span><span class="i18n" lang="en">Delete permanently</span></button></p>
  </div>
  <div class="status" id="del-status" role="status" aria-live="polite"></div>
</form>
<div class="note" id="del-fallback">
  <span class="i18n" lang="bn">অনলাইন ফর্ম কাজ না করলে, নিবন্ধিত নম্বর লিখে ইমেইল করুন — আমরা যাচাই করে ৭ কার্যদিবসের মধ্যে মুছে দেবো: </span>
  <span class="i18n" lang="en">If the online form doesn't work, email us from any address with your registered number — we'll verify and delete within 7 working days: </span>
  <a data-email href="mailto:support@hisabbondhu.com">support@hisabbondhu.com</a>
</div>
<script>
(function(){
  var lang=function(){return document.documentElement.getAttribute('data-lang')==='en'?'en':'bn'};
  var msgs={
    sent:{bn:'কোড পাঠানো হয়েছে। SMS দেখুন।',en:'Code sent. Check your SMS.'},
    done:{bn:'আপনার অ্যাকাউন্ট ও সব তথ্য মুছে ফেলা হয়েছে।',en:'Your account and all data have been deleted.'},
    invalid_phone:{bn:'সঠিক মোবাইল নম্বর দিন।',en:'Enter a valid mobile number.'},
    invalid_code:{bn:'কোডটি ভুল।',en:'Wrong code.'},
    code_expired:{bn:'কোডের মেয়াদ শেষ, আবার কোড নিন।',en:'Code expired, request a new one.'},
    too_many_attempts:{bn:'অনেকবার ভুল হয়েছে, নতুন কোড নিন।',en:'Too many attempts, request a new code.'},
    rate_limited:{bn:'একটু পরে আবার চেষ্টা করুন।',en:'Please wait a moment and try again.'},
    api_not_configured:{bn:'অনলাইন ফর্ম এখনো চালু হয়নি — নিচের ইমেইলে অনুরোধ পাঠান।',en:'Online form is not active yet — please email us below.'},
    error:{bn:'সমস্যা হয়েছে, আবার চেষ্টা করুন বা ইমেইল করুন।',en:'Something went wrong. Try again or email us.'}
  };
  var st=document.getElementById('del-status');
  function show(k,ok){var m=msgs[k]||msgs.error;st.textContent=m[lang()];st.className='status '+(ok?'ok':'err');}
  var phone=document.getElementById('phone'),otp=document.getElementById('otp'),cb=document.getElementById('confirm'),del=document.getElementById('do-delete');
  cb.addEventListener('change',function(){del.disabled=!cb.checked});
  document.getElementById('send-otp').addEventListener('click',function(){
    var b=this;b.disabled=true;
    hbApi('/v1/auth/request-otp',{method:'POST',body:JSON.stringify({phone:phone.value})})
      .then(function(){show('sent',true);document.getElementById('step-otp').classList.remove('hidden');otp.focus();})
      .catch(function(e){show(e.code||e.message);})
      .finally(function(){b.disabled=false;});
  });
  del.addEventListener('click',function(){
    del.disabled=true;
    hbApi('/v1/auth/verify-otp',{method:'POST',body:JSON.stringify({phone:phone.value,code:otp.value.trim(),device_name:'web-deletion'})})
      .then(function(t){return hbApi('/v1/account',{method:'DELETE',headers:{authorization:'Bearer '+t.access_token}});})
      .then(function(){show('done',true);document.getElementById('step-phone').classList.add('hidden');document.getElementById('step-otp').classList.add('hidden');})
      .catch(function(e){show(e.code||e.message);del.disabled=!cb.checked;});
  });
})();
</script>
"""
page("data-deletion.html", "ডেটা মুছে ফেলুন · Delete account & data — হিসাববন্ধু",
     "How to delete your HisabBondhu account and data", "delete",
     f'<article class="doc">{bi(DEL_BN, DEL_EN)}</article>' + DEL_FORM)

# ---------------------------------------------------------------- contact
CONTACT_INTRO = bi("""<h1>যোগাযোগ করুন</h1>
<p>প্রশ্ন, সমস্যা, পরামর্শ বা ডেটা-সংক্রান্ত অনুরোধ — আমরা সাহায্য করতে প্রস্তুত। সাধারণত ২ কার্যদিবসের মধ্যে উত্তর দেওয়া হয় (রবি–বৃহস্পতি, সকাল ১০টা–সন্ধ্যা ৬টা)।</p>
<p>ইমেইল: """ + EMAIL + """</p>""", """<h1>Contact us</h1>
<p>Questions, problems, suggestions or data requests — we're happy to help. We usually reply within 2 working days (Sun–Thu, 10am–6pm Bangladesh time).</p>
<p>Email: """ + EMAIL + """</p>""")
CONTACT_FORM = """
<form class="card" id="contact-form">
  <label for="c-name"><span class="i18n" lang="bn">আপনার নাম</span><span class="i18n" lang="en">Your name</span></label>
  <input id="c-name" required maxlength="100" autocomplete="name">
  <label for="c-reply"><span class="i18n" lang="bn">মোবাইল বা ইমেইল (উত্তরের জন্য)</span><span class="i18n" lang="en">Mobile or email (for our reply)</span></label>
  <input id="c-reply" maxlength="120">
  <label for="c-subject"><span class="i18n" lang="bn">বিষয়</span><span class="i18n" lang="en">Subject</span></label>
  <select id="c-subject">
    <option value="Problem">সমস্যা / Problem</option><option value="Suggestion">পরামর্শ / Suggestion</option>
    <option value="Account / data">অ্যাকাউন্ট ও ডেটা / Account &amp; data</option><option value="Other">অন্যান্য / Other</option>
  </select>
  <label for="c-message"><span class="i18n" lang="bn">মেসেজ</span><span class="i18n" lang="en">Message</span></label>
  <textarea id="c-message" rows="5" required minlength="5" maxlength="4000"></textarea>
  <input id="c-website" name="website" tabindex="-1" autocomplete="off" style="position:absolute;left:-9999px" aria-hidden="true">
  <p><button class="btn" type="submit"><span class="i18n" lang="bn">পাঠান</span><span class="i18n" lang="en">Send</span></button></p>
  <div class="status" id="c-status" role="status" aria-live="polite"></div>
</form>
<script>
document.getElementById('contact-form').addEventListener('submit',function(e){
  e.preventDefault();
  var en=document.documentElement.getAttribute('data-lang')==='en',st=document.getElementById('c-status');
  var reply=document.getElementById('c-reply').value.trim();
  var body={name:document.getElementById('c-name').value,subject:document.getElementById('c-subject').value,
    message:document.getElementById('c-message').value,source:'web',website:document.getElementById('c-website').value};
  if(reply.indexOf('@')>0) body.email=reply; else if(reply) body.phone=reply;
  hbApi('/v1/public/contact',{method:'POST',body:JSON.stringify(body)})
    .then(function(){st.textContent=en?'Thank you! We received your message.':'ধন্যবাদ! আপনার মেসেজ পেয়েছি।';st.className='status ok';e.target.reset();})
    .catch(function(){
      var cfg=window.HISAB_CONFIG||{};
      st.textContent=en?'Could not send online — opening your email app.':'অনলাইনে পাঠানো যায়নি — ইমেইল অ্যাপ খোলা হচ্ছে।';st.className='status err';
      location.href='mailto:'+cfg.supportEmail+'?subject='+encodeURIComponent('HisabBondhu: '+body.subject)+'&body='+encodeURIComponent(body.message+'\\n\\n— '+body.name+' '+reply);
    });
});
</script>
"""
page("contact.html", "যোগাযোগ · Contact — হিসাববন্ধু", "Contact HisabBondhu support", "contact",
     f'<section class="doc">{CONTACT_INTRO}</section>' + CONTACT_FORM)

page("404.html", "পাতা পাওয়া যায়নি · Not found — হিসাববন্ধু", "Page not found", "home",
     '<section class="doc">' + bi('<h1>পাতাটি পাওয়া যায়নি</h1><p><a href="index.html">হোমে ফিরে যান</a></p>',
                                  '<h1>Page not found</h1><p><a href="index.html">Back to home</a></p>') + '</section>')
print("pages written")
