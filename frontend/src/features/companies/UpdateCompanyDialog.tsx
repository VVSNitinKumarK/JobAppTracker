import { useEffect, useMemo } from "react";
import { z } from "zod";
import { useForm, useWatch } from "react-hook-form";
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

import type { CompanyRow } from "./types";
import { useUpdateCompany } from "./hooks";

const schema = z.object({
    companyName: z.string().min(1, "Company Name is required").max(120),
    careersUrl: z.string().url("Enter a valid URL"),
    lastVisitedOn: z.string().optional(),
    revisitAfterDays: z.number().int().min(1, "Min 1").max(20, "Max 20"),
    tagsText: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

function parseMMddyyyyToYmd(input: string): string | null {
    const trimmed = input.trim();
    if (!trimmed) {
        return null;
    }

    const date = parse(trimmed, "MM/dd/yyyy", new Date());
    if (!isValid(date)) {
        return null;
    }

    return format(date, "yyyy-MM-dd");
}

function ymdToMmddyyyy(ymd: string) {
    const date = new Date(`${ymd}T00:00:00`);
    return isValid(date) ? format(date, "MM/dd/yyyy") : "";
}

function tagsFromText(s?: string) {
    if (!s) {
        return [];
    }

    return s
        .split(",")
        .map((x) => x.trim())
        .filter((x) => x.length > 0);
}

type Properties = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    company: CompanyRow | null;
};

export function UpdateCompanyDialog({
    open,
    onOpenChange,
    company,
}: Properties) {
    const update = useUpdateCompany();

    const defaultValues: FormValues = useMemo(
        () => ({
            companyName: company?.companyName ?? "",
            careersUrl: company?.careersUrl ?? "",
            lastVisitedOn: company?.lastVisitedOn
                ? ymdToMmddyyyy(company.lastVisitedOn)
                : "",
            revisitAfterDays: company?.revisitAfterDays ?? 7,
            tagsText: (company?.tags ?? []).join(", "),
        }),
        [company]
    );

    const form = useForm<FormValues>({
        resolver: zodResolver(schema),
        defaultValues,
        mode: "onSubmit",
    });

    const { reset } = form;

    useEffect(() => {
        if (open) {
            reset(defaultValues);
        }
    }, [open, defaultValues, reset]);

    const {
        register,
        handleSubmit,
        setValue,
        control,
        formState: { errors },
    } = form;

    const lastVisitedText = useWatch({ control, name: "lastVisitedOn" });

    const onSubmit = handleSubmit(async (values) => {
        if (!company) {
            return;
        }

        const lastVisitedYmd = parseMMddyyyyToYmd(values.lastVisitedOn ?? "");
        if ((values.lastVisitedOn ?? "").trim() && !lastVisitedYmd) {
            form.setError("lastVisitedOn", {
                type: "manual",
                message: "Use MM/DD/YYYY or pick from calendar",
            });
            return;
        }

        await update.mutateAsync({
            companyId: company.companyId,
            payload: {
                companyName: values.companyName.trim(),
                careersUrl: values.careersUrl.trim(),
                lastVisitedOn: lastVisitedYmd ?? undefined,
                revisitAfterDays: values.revisitAfterDays,
                tags: tagsFromText(values.tagsText),
            },
        });

        onOpenChange(false);
    });

    return (
        <Dialog
            open={open}
            onOpenChange={(v) => !update.isPending && onOpenChange(v)}
        >
            <DialogContent className="sm:max0w-[520px]">
                <DialogHeader>
                    <DialogTitle>Update Company</DialogTitle>
                </DialogHeader>

                <form onSubmit={onSubmit} className="space-y-4">
                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Company Name
                        </label>
                        <Input
                            placeholder="Netflix"
                            {...register("companyName")}
                        />
                        {errors.companyName ? (
                            <div className="text-xs text-red-600">
                                {errors.companyName.message}
                            </div>
                        ) : null}
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Careers URL
                        </label>
                        <Input
                            placeholder="https://jobs.netflix.com"
                            {...register("companyName")}
                        />
                        {errors.careersUrl ? (
                            <div className="text-xs text-red-600">
                                {errors.careersUrl.message}
                            </div>
                        ) : null}
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Last Visited On (optional)
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
                                        aria-label="Pick date"
                                        className={cn(
                                            "absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-muted-foireground",
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
                                                lastVisitedText ?? ""
                                            )
                                                ? new Date(
                                                      `${parseMMddyyyyToYmd(
                                                          lastVisitedText ?? ""
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
                                                ymdToMmddyyyy(ymd),
                                                {
                                                    shouldValidate: true,
                                                }
                                            );
                                        }}
                                    />
                                </PopoverContent>
                            </Popover>
                        </div>

                        {errors.lastVisitedOn ? (
                            <div className="text-xs text-red-600">
                                {errors.lastVisitedOn.message}
                            </div>
                        ) : (
                            <div className="text-xs text-muted-foreground">
                                Type MM/DD/YYYY or use the calendar
                            </div>
                        )}
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Revist After Days
                        </label>
                        <Input
                            type="number"
                            min={1}
                            max={20}
                            {...register("revisitAfterDays", {
                                valueAsNumber: true,
                            })}
                        />
                        {errors.revisitAfterDays ? (
                            <div className="text-xs text-red-600">
                                {errors.revisitAfterDays.message}
                            </div>
                        ) : (
                            <div className="text-xs text-muted-foreground">
                                Allowed: 1 to 20
                            </div>
                        )}
                    </div>

                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">
                            Tags (comma-separate)
                        </label>
                        <Input
                            placeholder="bigtech, backend, platform"
                            {...register("tagsText")}
                        />
                    </div>

                    <div className="flex items-center justify-end gap-2 pt-2">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => onOpenChange(false)}
                            disabled={update.isPending}
                        >
                            Cancel
                        </Button>
                        <Button
                            type="submit"
                            disabled={update.isPending || !company}
                        >
                            {update.isPending ? "Updating..." : "Update"}
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
