import React, { InputHTMLAttributes, forwardRef, ReactNode } from 'react';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  fullWidth?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className = '', label, error, helperText, leftIcon, rightIcon, fullWidth = true, ...props }, ref) => {
    return (
      <div className={`${fullWidth ? 'w-full' : 'inline-block'} space-y-1`}>
        {label && (
          <label className="block text-xs font-semibold text-[#0F172A] tracking-wide">
            {label}
          </label>
        )}

        <div className="relative flex items-center">
          {leftIcon && (
            <span className="absolute left-2.5 flex items-center pointer-events-none text-[#64748B]">
              {leftIcon}
            </span>
          )}

          <input
            ref={ref}
            className={`
              w-full h-9 rounded-md bg-white border text-xs sm:text-sm text-[#0F172A] placeholder-[#64748B]
              transition-all duration-150 outline-none
              ${leftIcon ? 'pl-8' : 'pl-3'}
              ${rightIcon ? 'pr-8' : 'pr-3'}
              ${error ? 'border-[#BA1A1A] focus:border-[#BA1A1A] focus:ring-1 focus:ring-[#BA1A1A]' : 'border-[#CBD5E1] focus:border-[#0F172A] focus:ring-1 focus:ring-[#0F172A]'}
              disabled:bg-slate-50 disabled:text-slate-400 disabled:cursor-not-allowed
              ${className}
            `}
            {...props}
          />

          {rightIcon && (
            <span className="absolute right-2.5 flex items-center text-[#64748B]">
              {rightIcon}
            </span>
          )}
        </div>

        {error && (
          <p className="text-[11px] font-medium text-[#BA1A1A]">{error}</p>
        )}
        {!error && helperText && (
          <p className="text-[11px] text-[#64748B]">{helperText}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
export default Input;