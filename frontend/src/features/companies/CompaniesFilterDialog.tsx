import { useEffect, useMemo } from "react";
import { z } from "zod";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { parseMMddyyyyToYmd, ymdToMmddyyyy } from "@/lib/dateUtils";

import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { DatePickerField } from "@/components/form/DatePickerField";
import { cn } from "@/lib/utils";

import { useTagOptions } from "./useTagOptions";

const schema = z.object({
    lastVisitedOn: z.string().optional(),
    nextVisitOn: z.string().optional(),
    tags: z.array(z.string()).catch([]),
});

export type CompanyFilters = {
    lastVisitedOnYmd: string | null;
    nextVisitOnYmd: string | null;
    tagsAny: string[];
};

type FormValues = z.infer<typeof schema>;

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
    const tagOptions = useTagOptions();

    const defaultValues: FormValues = useMemo(
        () => ({
            lastVisitedOn: value.lastVisitedOnYmd
                ? ymdToMmddyyyy(value.lastVisitedOnYmd)
                : "",
            nextVisitOn: value.nextVisitOnYmd
                ? ymdToMmddyyyy(value.nextVisitOnYmd)
                : "",
            tags: value.tagsAny ?? [],
        }),
        [value]
    );

    // 1) Create form FIRST
    const form = useForm<FormValues>({
        resolver: zodResolver(schema),
        defaultValues,
        mode: "onSubmit",
    });

    // 2) THEN destructure
    const {
        reset,
        register,
        handleSubmit,
        setValue,
        formState,
        control,
    } = form;

    // reactive selection list
    const selectedTags = useWatch({ control, name: "tags" }) ?? [];
    const lastVisitedText = useWatch({ control, name: "lastVisitedOn" });
    const nextVisitText = useWatch({ control, name: "nextVisitOn" });

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
            tagsAny: values.tags ?? [],
        });
        onOpenChange(false);
    });

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[520px]">
                <DialogHeader>
                    <DialogTitle>Filters</DialogTitle>
                </DialogHeader>

                <form onSubmit={submit} className="space-y-4">
                    <DatePickerField
                        label="Last Visited"
                        register={register("lastVisitedOn")}
                        setValue={setValue}
                        value={lastVisitedText}
                        error={formState.errors.lastVisitedOn?.message as string}
                        ariaLabel="Pick last visited date"
                    />

                    <DatePickerField
                        label="Next Visit"
                        register={register("nextVisitOn")}
                        setValue={setValue}
                        value={nextVisitText}
                        error={formState.errors.nextVisitOn?.message as string}
                        ariaLabel="Pick next visit date"
                    />

                    {/* Tags */}
                    <div className="space-y-1.5">
                        <label className="text-sm font-medium">Tags</label>

                        <div className="flex flex-wrap gap-2 rounded-md border p-2 max-h-40 overflow-auto">
                            {tagOptions.map((t) => {
                                const selected = selectedTags.includes(t.key);

                                return (
                                    <button
                                        key={t.key}
                                        type="button"
                                        onClick={() => {
                                            const next = selected
                                                ? selectedTags.filter(
                                                      (k) => k !== t.key
                                                  )
                                                : [...selectedTags, t.key];

                                            setValue("tags", next, {
                                                shouldValidate: true,
                                            });
                                        }}
                                        className={cn(
                                            "rounded-full border px-3 py-1 text-sm",
                                            selected
                                                ? "bg-foreground text-background border-foreground"
                                                : "bg-background hover:bg-muted"
                                        )}
                                    >
                                        {t.label}
                                    </button>
                                );
                            })}
                        </div>

                        <div className="text-xs text-muted-foreground">
                            Multiple tags match with OR (any-of).
                        </div>
                    </div>

                    {/* Buttons */}
                    <div className="flex items-center justify-between pt-2">
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={() => {
                                onClear();
                                reset({
                                    lastVisitedOn: "",
                                    nextVisitOn: "",
                                    tags: [],
                                });
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
