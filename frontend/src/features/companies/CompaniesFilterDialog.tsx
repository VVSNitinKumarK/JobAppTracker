import { useEffect, useMemo } from "react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { format, isValid, parse } from "date-fns";
import { Calendar as CalendarIcon } from "lucide-react";

import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Calendar } from "@/components/ui/calendar";
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";

const schema = z.object({
    lastVisitedOn: z.string().optional(),
    nextVisitOn: z.string().optional(),
    tagsText: z.string().optional(),
});

export type CompanyFilters = {
    lastVisitedOnYmd?: string | null;
    nextVisitOnYmd?: string | null;
    tagsAny?: string[];
};

type FormValues = z.infer<typeof schema>;

function parseMMddyyyyToYmd(input?: string): string | null {
    const trimmed = (input ?? "").trim();
    if (!trimmed) {
        return null;
    }

    const date = parse(trimmed, "MM/dd/yyyy", new Date());
    if (!isValid(date)) {
        return null;
    }

    return format(date, "yyyy-MM-dd");
}

function ymdTOMmddyyyy(ymd: string) {
    const date = new Date(`${ymd}T00:00:00`);
    return isValid(date) ? format(date, "MM/dd/yyyy") : "";
}

function tagsFromText(s?: string) {
    if (!s) {
        return [];
    }

    return s
        .split(",")
        .map((x) => x.trim().toLowerCase())
        .filter((x) => x.length > 0);
}

type Properties = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    value: CompanyFilters;
    onApply: (next: CompanyFilters) => void;
    onClear: () => void;
};

export function CompaniesFilterDialog({
    open,
    onOpenChange,
    value,
    onApply,
    onClear,
}: Properties) {
    const defaultValues: FormValues = useMemo(
        () => ({
            lastVisitedOn: value.lastVisitedOnYmd
                ? ymdTOMmddyyyy(value.lastVisitedOnYmd)
                : "",
            nextVisitOn: value.nextVisitOnYmd
                ? ymdTOMmddyyyy(value.nextVisitOnYmd)
                : "",
            tagsText: (value.tagsAny ?? []).join(", "),
        }),
        [value]
    );

    const form = useForm<FormValues>({
        resolver: zodResolver(schema),
        defaultValues,
        mode: "onSubmit",
    });

    const { reset, register, handleSubmit, setValue, formState } = form;

    useEffect(() => {
        if (open) {
            reset(defaultValues);
        }
    }, [open, defaultValues, reset]);

    const submit = handleSubmit((values) => {
        const lastYmd = parseMMddyyyyToYmd(values.lastVisitedOn);
        const nextYmd = parseMMddyyyyToYmd(values.nextVisitOn);

        if ((values.lastVisitedOn ?? "").trim() && !lastYmd) {
            form.setError("lastVisitedOn", {
                type: "manual",
                message: "Use MM/DD/YYYY or pick from calendar",
            });
            return;
        }
        if ((values.nextVisitOn ?? "").trim() && !nextYmd) {
            form.setError("nextVisitOn", {
                type: "manual",
                message: "Use MM/DD/YYYY or pick from calendar",
            });
            return;
        }

        onApply({
            lastVisitedOnYmd: lastYmd,
            nextVisitOnYmd: nextYmd,
            tagsAny: tagsFromText(values.tagsText),
        });
        onOpenChange(false);
    });

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[520px]">
                <DialogHeader>
                    <DialogTitle>filters</DialogTitle>
                </DialogHeader>

                <form onSubmit={submit} className="space-y-4">
                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Last Visited
                        </label>
                        <div className="relative">
                            <Input
                                placeholder="MM/DD/YYYY"
                                {...register("lastVisitedOn")}
                                className="pr-10"
                            />
                            <Popover>
                                <PopoverTrigger asChild>
                                    <button
                                        type="button"
                                        aria-label="Pick last visited date"
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
                                        selected={
                                            parseMMddyyyyToYmd(
                                                form.getValues("lastVisitedOn")
                                            )
                                                ? new Date(
                                                      `${parseMMddyyyyToYmd(
                                                          form.getValues(
                                                              "lastVisitedOn"
                                                          )
                                                      )}T00:00:00`
                                                  )
                                                : undefined
                                        }
                                        onSelect={(date) => {
                                            if (!date) {
                                                return;
                                            }

                                            const ymd = format(
                                                date,
                                                "yyyy-MM-dd"
                                            );
                                            setValue(
                                                "lastVisitedOn",
                                                ymdTOMmddyyyy(ymd),
                                                {
                                                    shouldValidate: true,
                                                }
                                            );
                                        }}
                                    />
                                </PopoverContent>
                            </Popover>
                        </div>
                        {formState.errors.lastVisitedOn ? (
                            <div className="text-xs text-red-600">
                                {
                                    formState.errors.lastVisitedOn
                                        .message as string
                                }
                            </div>
                        ) : null}
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Next Visit
                        </label>
                        <div className="relative">
                            <Input
                                placeholder="MM/DD/YYYY"
                                {...register("nextVisitOn")}
                                className="pr-10"
                            />
                            <Popover>
                                <PopoverTrigger asChild>
                                    <button
                                        type="button"
                                        aria-label="Pick next visit date"
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
                                        selected={
                                            parseMMddyyyyToYmd(
                                                form.getValues("nextVisitOn")
                                            )
                                                ? new Date(
                                                      `${parseMMddyyyyToYmd(
                                                          form.getValues(
                                                              "nextVisitOn"
                                                          )
                                                      )}T00:00:00`
                                                  )
                                                : undefined
                                        }
                                        onSelect={(date) => {
                                            if (!date) {
                                                return;
                                            }

                                            const ymd = format(
                                                date,
                                                "yyyy-MM-dd"
                                            );
                                            setValue(
                                                "nextVisitOn",
                                                ymdTOMmddyyyy(ymd),
                                                {
                                                    shouldValidate: true,
                                                }
                                            );
                                        }}
                                    />
                                </PopoverContent>
                            </Popover>
                        </div>
                        {formState.errors.nextVisitOn ? (
                            <div className="text-xs text-red-600">
                                {formState.errors.nextVisitOn.message as string}
                            </div>
                        ) : null}
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Tags (comma-separated, matches any)
                        </label>
                        <Input
                            placeholder="bigtech, backend"
                            {...register("tagsText")}
                        />
                        <div className="text-xs text-muted-foreground">
                            Multiple tags match with OR (any-of).
                        </div>
                    </div>

                    <div className="flex items-center justify-between pt-2">
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={() => {
                                onClear();
                                onOpenChange(false);
                            }}
                        >
                            Clear filters
                        </Button>

                        <div className="flex items-center gap-2">
                            <Button
                                type="button"
                                variant="outline"
                                onClick={() => onOpenChange(false)}
                            >
                                Cancel
                            </Button>
                            <Button type="submit">Apply filters</Button>
                        </div>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
