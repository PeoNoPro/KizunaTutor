import React from 'react';

export interface GrammarCardProps {
  level: string; // "N5", "N4", ...
  pattern: string; // "~ha"
  reading: string; // "wa"
  meaning: string; // "Thì; là; đóng vai trò làm chủ đề chính của câu"
  tags?: string[]; // ["Đời thường", "Du lịch"]
  onViewDetail?: () => void;
  className?: string;
}

export const GrammarCard: React.FC<GrammarCardProps> = ({
  level,
  pattern,
  reading,
  meaning,
  tags = [],
  onViewDetail,
  className = ''
}) => {
  return (
    <div
      onClick={onViewDetail}
      className={`bg-white border-2 border-[#0F172A] rounded-[12px] p-4 sm:p-5 shadow-[3px_3px_0px_0px_#0F172A] transition-all hover:translate-x-[-1px] hover:translate-y-[-1px] flex flex-col justify-between group cursor-pointer ${className}`}
    >
      <div>
        {/* DÒNG 1: Tên ngữ pháp bên trái, Duy nhất Badge JLPT bên phải sát lề */}
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-baseline gap-2 min-w-0">
            <h3 className="text-lg sm:text-xl font-bold text-[#0F172A] font-japanese group-hover:text-[#831843] transition truncate">
              {pattern}
            </h3>
            <span className="text-xs sm:text-sm text-[#64748B] font-mono shrink-0">({reading})</span>
          </div>
          {/* Badge Cấp độ JLPT chuẩn hóa phong cách Shodo */}
          <span className="px-2.5 py-0.5 text-xs font-bold text-[#0F172A] bg-[#FEF3C7] border border-[#0F172A] rounded-[4px] shadow-[1px_1px_0px_0px_#0F172A] shrink-0">
            {level}
          </span>
        </div>

        {/* DÒNG 2: Ý nghĩa ngữ pháp cốt lõi */}
        <p className="mt-2.5 text-xs sm:text-sm text-[#334155] leading-relaxed line-clamp-2">
          {meaning}
        </p>

        {/* DÒNG 3: Các thẻ chủ đề / ngữ cảnh (Đặt sau phần giải nghĩa) */}
        {tags && tags.length > 0 && (
          <div className="mt-3 flex flex-wrap items-center gap-1.5">
            {tags.map((tag, idx) => (
              <span
                key={idx}
                className="px-2 py-0.5 text-[11px] font-medium text-[#64748B] bg-[#F1F5F9] border border-[#CBD5E1] rounded-full"
              >
                #{tag}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* DÒNG 4: Nút xem chi tiết */}
      <div className="mt-4 pt-3 border-t border-[#E2E8F0] flex justify-end">
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onViewDetail?.();
          }}
          className="text-xs font-semibold text-[#006194] group-hover:text-[#007BB9] flex items-center gap-1 transition-colors"
        >
          <span>Xem chi tiết công thức &amp; ví dụ</span>
          <span className="transition-transform group-hover:translate-x-0.5 font-bold">→</span>
        </button>
      </div>
    </div>
  );
};

export default GrammarCard;