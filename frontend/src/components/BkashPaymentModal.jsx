import { useState, useEffect } from 'react'
import { api } from '../api/client.js'
import { plain } from '../utils/format.js'

export function BkashPaymentModal({ invoice, onDone, onClose }) {
  const [step, setStep] = useState('INIT') // 'INIT', 'PHONE', 'OTP', 'PIN', 'PROCESSING', 'SUCCESS'
  const [phone, setPhone] = useState('01712345678')
  const [otp, setOtp] = useState('1234')
  const [pin, setPin] = useState('12345')
  const [agree, setAgree] = useState(true)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [intentData, setIntentData] = useState(null)
  const [receiptSummary, setReceiptSummary] = useState(null)
  const [resendTimer, setResendTimer] = useState(30)

  useEffect(() => {
    let interval = null
    if (step === 'OTP' && resendTimer > 0) {
      interval = setInterval(() => setResendTimer((t) => t - 1), 1000)
    }
    return () => clearInterval(interval)
  }, [step, resendTimer])

  // ১. ক্রিয়েট পেমেন্ট ইনটেন্ট
  async function handleInitiatePayment(e) {
    if (e) e.preventDefault()
    if (!phone || phone.length < 11) {
      setError('সঠিক ১১ ডিজিটের বিকাশ মোবাইল নম্বর প্রদান করুন')
      return
    }
    if (!agree) {
      setError('শর্তাবলীর সাথে সম্মতি আবশ্যক')
      return
    }

    setError('')
    setBusy(true)
    try {
      const res = await api('/api/payments/create-intent', {
        method: 'POST',
        body: {
          invoiceId: invoice.id,
          customerPhone: phone,
          gatewayProvider: 'BKASH',
          idempotencyKey: `INV-${invoice.id}-${Date.now()}`
        }
      })
      setIntentData(res)
      setStep('OTP')
      setResendTimer(30)
    } catch (err) {
      setError(err.message || 'পেমেন্ট ইনটেন্ট তৈরি করতে ব্যর্থ হয়েছে')
    } finally {
      setBusy(false)
    }
  }

  // ২. ওটিপি ভেরিফিকেশন
  function handleConfirmOtp(e) {
    if (e) e.preventDefault()
    if (!otp || otp.length < 4) {
      setError('সঠিক ৪ বা ৬ ডিজিটের ভেরিফিকেশন কোড লিখুন')
      return
    }
    setError('')
    setStep('PIN')
  }

  // ৩. পিন দিয়ে পেমেন্ট এক্সেকিউট
  async function handleConfirmPin(e) {
    if (e) e.preventDefault()
    if (!pin || pin.length < 4) {
      setError('সঠিক ৫ ডিজিটের বিকাশ গোপন পিন লিখুন')
      return
    }

    setError('')
    setBusy(true)
    setStep('PROCESSING')

    try {
      const res = await api('/api/payments/execute', {
        method: 'POST',
        body: {
          trxId: intentData.trxId,
          gatewayPaymentId: intentData.gatewayPaymentId,
          otp: otp,
          pin: pin
        }
      })
      setReceiptSummary(res)
      setStep('SUCCESS')
    } catch (err) {
      setError(err.message || 'পেমেন্ট সম্পন্ন করতে সমস্যা হয়েছে')
      setStep('PIN')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl transition-all">
        {/* bKash Header */}
        <div className="bg-[#e2136e] px-6 py-4 text-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white shadow-sm">
                {/* bKash Bird Stylized Icon */}
                <svg viewBox="0 0 48 48" className="h-7 w-7" fill="none">
                  <path
                    d="M26.4 5.2L42.8 19.8L35.6 24.2L22.2 12.4L26.4 5.2Z"
                    fill="#E2136E"
                  />
                  <path
                    d="M4.8 28.5L21.4 13.2L35.2 24.5L18.6 39.8L4.8 28.5Z"
                    fill="#E2136E"
                  />
                  <path
                    d="M19.1 40.2L35.8 24.8L43.2 29.2L26.5 44.6L19.1 40.2Z"
                    fill="#DF106B"
                  />
                </svg>
              </div>
              <div>
                <h3 className="text-lg font-bold tracking-tight">bKash Payment</h3>
                <p className="text-xs text-pink-100">বাংলাদেশ ব্যাংক অনুমোদিত গেটওয়ে</p>
              </div>
            </div>
            {step !== 'PROCESSING' && (
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg p-1.5 text-pink-100 hover:bg-white/10 hover:text-white"
              >
                ✕
              </button>
            )}
          </div>
        </div>

        {/* Invoice Brief Banner */}
        <div className="bg-pink-50/70 border-b border-pink-100 px-6 py-3">
          <div className="flex items-center justify-between text-sm">
            <div>
              <p className="text-xs text-gray-500">ইনভয়েস: {invoice.invoiceNumber}</p>
              <p className="font-semibold text-gray-900">{invoice.studentName}</p>
            </div>
            <div className="text-right">
              <p className="text-xs text-gray-500">পরিশোধের পরিমাণ</p>
              <p className="text-lg font-extrabold text-[#e2136e]">
                ৳ {plain(invoice.dueAmount)}
              </p>
            </div>
          </div>
        </div>

        {/* Modal Body */}
        <div className="p-6">
          {error && (
            <div className="mb-4 rounded-lg bg-rose-50 p-3 text-xs font-medium text-rose-700 border border-rose-200">
              ⚠️ {error}
            </div>
          )}

          {/* STEP 1: Phone Number Input */}
          {(step === 'INIT' || step === 'PHONE') && (
            <form onSubmit={handleInitiatePayment} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">
                  আপনার বিকাশ অ্যাকাউন্ট নম্বর (Your bKash Account Number)
                </label>
                <div className="relative">
                  <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-sm text-gray-400 font-mono">
                    🇧🇩 +88
                  </span>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="01XXXXXXXXX"
                    maxLength={11}
                    required
                    className="w-full rounded-xl border border-gray-300 py-2.5 pl-20 pr-4 text-sm font-semibold tracking-wider text-gray-900 focus:border-[#e2136e] focus:outline-none focus:ring-1 focus:ring-[#e2136e]"
                  />
                </div>
                <p className="mt-1 text-[11px] text-gray-500">
                  সিমুলেটর মোড: ডিফল্ট পরীক্ষামূলক নম্বর দেওয়া আছে।
                </p>
              </div>

              <div className="flex items-start gap-2 pt-1">
                <input
                  type="checkbox"
                  id="bkash-agree"
                  checked={agree}
                  onChange={(e) => setAgree(e.target.checked)}
                  className="mt-1 h-4 w-4 rounded border-gray-300 text-[#e2136e] focus:ring-[#e2136e]"
                />
                <label htmlFor="bkash-agree" className="text-xs text-gray-600 leading-snug">
                  আমি বিকাশ পেমেন্ট গেটওয়ের{' '}
                  <span className="text-[#e2136e] underline cursor-pointer">
                    শর্তাবলী ও নিয়মাবলী
                  </span>{' '}
                  মেনে নিচ্ছি।
                </label>
              </div>

              <div className="flex items-center justify-between pt-3 gap-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="w-1/2 rounded-xl border border-gray-300 py-2.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  বাতিল করুন
                </button>
                <button
                  type="submit"
                  disabled={busy}
                  className="w-1/2 rounded-xl bg-[#e2136e] py-2.5 text-xs font-bold text-white shadow hover:bg-[#c20f5e] disabled:opacity-50"
                >
                  {busy ? 'লোড হচ্ছে...' : 'নিশ্চিত করুন'}
                </button>
              </div>
            </form>
          )}

          {/* STEP 2: OTP Verification */}
          {step === 'OTP' && (
            <form onSubmit={handleConfirmOtp} className="space-y-4">
              <div className="text-center pb-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-pink-100 text-[#e2136e] mb-2 font-bold text-lg">
                  💬
                </div>
                <h4 className="text-sm font-bold text-gray-900">ভেরিফিকেশন কোড লিখুন</h4>
                <p className="text-xs text-gray-500 mt-0.5">
                  একটি ওটিপি কোড পাঠানো হয়েছে <span className="font-semibold text-gray-800">{phone}</span> নম্বরে
                </p>
              </div>

              <div>
                <input
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="1234"
                  maxLength={6}
                  required
                  autoFocus
                  className="w-full rounded-xl border border-gray-300 py-3 text-center text-xl font-bold tracking-[0.4em] text-gray-900 focus:border-[#e2136e] focus:outline-none focus:ring-1 focus:ring-[#e2136e]"
                />
                <div className="mt-2 flex items-center justify-between text-xs text-gray-500">
                  <span>সিমুলেশন কোড: <b>1234</b></span>
                  {resendTimer > 0 ? (
                    <span className="text-gray-400">পুনরায় কোড পাঠান ({resendTimer}s)</span>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setResendTimer(30)}
                      className="text-[#e2136e] font-semibold hover:underline"
                    >
                      কোড পুনরায় পাঠান
                    </button>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between pt-2 gap-3">
                <button
                  type="button"
                  onClick={() => setStep('PHONE')}
                  className="w-1/2 rounded-xl border border-gray-300 py-2.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  পেছনে যান
                </button>
                <button
                  type="submit"
                  className="w-1/2 rounded-xl bg-[#e2136e] py-2.5 text-xs font-bold text-white shadow hover:bg-[#c20f5e]"
                >
                  কোড নিশ্চিত করুন
                </button>
              </div>
            </form>
          )}

          {/* STEP 3: Enter PIN */}
          {step === 'PIN' && (
            <form onSubmit={handleConfirmPin} className="space-y-4">
              <div className="text-center pb-2">
                <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-pink-100 text-[#e2136e] mb-2 font-bold text-lg">
                  🔒
                </div>
                <h4 className="text-sm font-bold text-gray-900">বিকাশ পিন (PIN) লিখুন</h4>
                <p className="text-xs text-gray-500 mt-0.5">
                  ফি পরিশোধ নিশ্চিত করতে আপনার ৫ ডিজিটের গোপন পিন প্রদান করুন
                </p>
              </div>

              <div>
                <input
                  type="password"
                  value={pin}
                  onChange={(e) => setPin(e.target.value)}
                  placeholder="•••••"
                  maxLength={5}
                  required
                  autoFocus
                  className="w-full rounded-xl border border-gray-300 py-3 text-center text-xl font-bold tracking-[0.5em] text-gray-900 focus:border-[#e2136e] focus:outline-none focus:ring-1 focus:ring-[#e2136e]"
                />
                <p className="mt-1.5 text-center text-[11px] text-gray-400">
                  সিমুলেটরে পরীক্ষামূলক পিন: <b>12345</b>
                </p>
              </div>

              <div className="flex items-center justify-between pt-2 gap-3">
                <button
                  type="button"
                  onClick={() => setStep('OTP')}
                  className="w-1/2 rounded-xl border border-gray-300 py-2.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  পেছনে যান
                </button>
                <button
                  type="submit"
                  disabled={busy}
                  className="w-1/2 rounded-xl bg-[#e2136e] py-2.5 text-xs font-bold text-white shadow hover:bg-[#c20f5e] disabled:opacity-50"
                >
                  {busy ? 'যাচাই হচ্ছে...' : 'পেমেন্ট সম্পন্ন করুন'}
                </button>
              </div>
            </form>
          )}

          {/* STEP 4: Processing Animation */}
          {step === 'PROCESSING' && (
            <div className="py-8 text-center space-y-4">
              <div className="relative mx-auto h-16 w-16">
                <div className="absolute inset-0 rounded-full border-4 border-pink-200"></div>
                <div className="absolute inset-0 rounded-full border-4 border-[#e2136e] border-t-transparent animate-spin"></div>
              </div>
              <div>
                <h4 className="text-base font-bold text-gray-900">পেমেন্ট প্রসেসিং হচ্ছে...</h4>
                <p className="text-xs text-gray-500 mt-1">
                  দয়া করে অপেক্ষা করুন, বিকাশ পেমেন্ট গেটওয়ের সাথে ট্রানজেকশন যাচাই করা হচ্ছে
                </p>
              </div>
            </div>
          )}

          {/* STEP 5: Success & Receipt */}
          {step === 'SUCCESS' && receiptSummary && (
            <div className="space-y-4">
              <div className="text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 mb-2">
                  <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <h4 className="text-base font-bold text-gray-900">পেমেন্ট সফল হয়েছে!</h4>
                <p className="text-xs text-emerald-600 font-medium">ফি সফলভাবে পরিশোধিত ও রসিদ তৈরি হয়েছে</p>
              </div>

              <div className="rounded-xl border border-gray-200 bg-gray-50/70 p-4 space-y-2.5 text-xs">
                <div className="flex justify-between border-b pb-2">
                  <span className="text-gray-500">অফিসিয়াল রসিদ নম্বর:</span>
                  <span className="font-bold text-indigo-700">{receiptSummary.receiptNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">বিকাশ TrxID:</span>
                  <span className="font-mono font-semibold text-gray-900">{receiptSummary.gatewayTrxId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">সিস্টেম ট্রানজেকশন ID:</span>
                  <span className="font-mono text-gray-700">{receiptSummary.trxId}</span>
                </div>
                <div className="flex justify-between border-t pt-2">
                  <span className="font-medium text-gray-700">পরিশোধিত অর্থ:</span>
                  <span className="text-sm font-bold text-emerald-700">৳ {plain(receiptSummary.amountPaid)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">অবশিষ্ট বকেয়া:</span>
                  <span className="font-semibold text-gray-900">৳ {plain(receiptSummary.remainingDue)}</span>
                </div>
              </div>

              <div className="flex items-center justify-between pt-2 gap-3">
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="w-1/2 rounded-xl border border-gray-300 py-2.5 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  🖨️ প্রিন্ট করুন
                </button>
                <button
                  type="button"
                  onClick={onDone}
                  className="w-1/2 rounded-xl bg-emerald-600 py-2.5 text-xs font-bold text-white shadow hover:bg-emerald-700"
                >
                  সম্পন্ন (Done)
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
