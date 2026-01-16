import { format } from "date-fns";
import { Calendar as CalendarIcon } from "lucide-react";
import type { UseFormRegisterReturn, UseFormSetValue } from "react-hook-form";

import { Input } from "@/components/ui/input";
import { Calendar } from "@/components/ui/calendar";
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { parseMMddyyyyToYmd, ymdToMmddyyyy } from "@/lib/dateUtils";

type Properties = {
    label: string;
    placeholder?: string;
    register: UseFormRegisterReturn;
    setValue: UseFormSetValue<any>;
    value: string | undefined;
    error?: string;
    helperText?: string;
    ariaLabel?: string;
};

export function DatePickerField({
    label,
    placeholder = "MM/DD/YYYY",
    register,
    setValue,
    value,
    error,
    helperText,
    ariaLabel = "Pick date",
}: Properties) {
    const ymd = parseMMddyyyyToYmd(value ?? "");
    const selectedDate = ymd ? new Date(`${ymd}T00:00:00`) : undefined;

    return (
        <div className="space-y-1.5">
            <label className="text-sm font-medium">{label}</label>

            <div className="relative">
                <Input
                    placeholder={placeholder}
                    {...register}
                    className="pr-10"
                />

                <Popover>
                    <PopoverTrigger asChild>
                        <button
                            type="button"
                            aria-label={ariaLabel}
                            className={cn(
                                "absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-muted-foreground",
                                "hover:bg-muted hover:text-foreground",
                                "focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
                            )}
                        >
                            <CalendarIcon className="h-4 w-4" />
                        </button>
                    </PopoverTrigger>

                    <PopoverContent align="end" className="p-2">
                        <Calendar
                            mode="single"
                            selected={selectedDate}
                            onSelect={(date) => {
                                if (!date) {
                                    return;
                                }

                                const ymd = format(date, "yyyy-MM-dd");
                                setValue(register.name, ymdToMmddyyyy(ymd), {
                                    shouldValidate: true,
                                });
                            }}
                        />
                    </PopoverContent>
                </Popover>
            </div>

            {error ? (
                <div className="text-xs text-red-600">{error}</div>
            ) : helperText ? (
                <div className="text-xs text-muted-foreground">
                    {helperText}
                </div>
            ) : null}
        </div>
    );
}
