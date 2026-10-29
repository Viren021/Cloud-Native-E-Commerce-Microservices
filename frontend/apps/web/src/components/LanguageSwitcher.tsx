
"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter, usePathname } from "@/i18n/navigation";
import { useTransition } from "react";
import { cn } from "@ecommerce/lib/utils";
import { uiStyles as s } from "./ui/ui.styles";

const SUPPORTED_LOCALES = ["en", "hi"] as const;

type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

export default function LanguageSwitcher() {
  const locale = useLocale();
  const t = useTranslations("Lang");
  const router = useRouter();
  const pathname = usePathname();
  const [isPending, startTransition] = useTransition();

  const switchTo = (nextLocale: SupportedLocale) => {
    if (locale === nextLocale) return;

    startTransition(() => {
      router.replace(pathname, { locale: nextLocale });
    });
  };

  return (
      <div className={s.langWrap} title={t("switchLabel")}>
        {SUPPORTED_LOCALES.map((loc) => (
            <button
                type="button"
                key={loc}
                onClick={() => switchTo(loc)}
                disabled={isPending || locale === loc}
                aria-pressed={locale === loc}
                className={cn(
                    s.langBtnBase,
                    locale === loc ? s.langBtnActive : s.langBtnIdle
                )}
            >
              {t(loc)}
            </button>
        ))}
      </div>
  );
}
