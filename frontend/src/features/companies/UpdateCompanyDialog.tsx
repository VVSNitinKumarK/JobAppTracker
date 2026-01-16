import { useEffect, useMemo, useRef, useState } from "react";
import { z } from "zod";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { parseMMddyyyyToYmd, ymdToMmddyyyy } from "@/lib/dateUtils";

import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FormField } from "@/components/form/FormField";
import { DatePickerField } from "@/components/form/DatePickerField";

import type { CompanyRow } from "./types";
import { useUpdateCompany } from "./hooks";
import { TagMultiSelect, type TagOption } from "./TagMultiSelect";
import { useTagOptions } from "./useTagOptions";

const schema = z.object({
    companyName: z.string().min(1, "Company Name is required").max(120),
    careersUrl: z.string().url("Enter a valid URL"),
    lastVisitedOn: z.string().optional(),
    revisitAfterDays: z.number().int().min(1, "Min 1").max(20, "Max 20"),
    tags: z.array(
        z.object({
            key: z.string().min(1),
            label: z.string().min(1),
        })
    ),
});

type FormValues = z.infer<typeof schema>;

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
    const tagOptions = useTagOptions();
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const defaultValues: FormValues = useMemo(() => {
        const companyTagKeys = (company?.tags ?? []).map((t) => t.tagKey);

        // show nice casing/label if exists in /tags results, else fallback label=key
        const selectedTags: TagOption[] = companyTagKeys
            .filter((tag) => tag.length > 0)
            .map(
                (k) =>
                    tagOptions.find((o) => o.key === k) ?? { key: k, label: k }
            );

        return {
            companyName: company?.companyName ?? "",
            careersUrl: company?.careersUrl ?? "",
            lastVisitedOn: company?.lastVisitedOn
                ? ymdToMmddyyyy(company.lastVisitedOn)
                : "",
            revisitAfterDays: company?.revisitAfterDays ?? 7,
            tags: selectedTags,
        };
    }, [company, tagOptions]);

    const form = useForm<FormValues>({
        resolver: zodResolver(schema),
        defaultValues,
        mode: "onSubmit",
    });

    const { reset } = form;
    const prevOpenRef = useRef(false);

    useEffect(() => {
        // Only reset when transitioning from closed to open
        if (open && !prevOpenRef.current) {
            reset(defaultValues);
        }
        prevOpenRef.current = open;
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

        try {
            setErrorMessage(null);
            await update.mutateAsync({
                companyId: company.companyId,
                payload: {
                    companyName: values.companyName.trim(),
                    careersUrl: values.careersUrl.trim(),
                    lastVisitedOn: lastVisitedYmd ?? undefined,
                    revisitAfterDays: values.revisitAfterDays,
                    tags: (values.tags ?? []).map((tag) => tag.key),
                },
            });

            onOpenChange(false);
        } catch (error) {
            setErrorMessage(
                error instanceof Error
                    ? error.message
                    : "Failed to update company"
            );
        }
    });

    const selectedTags = useWatch({ control, name: "tags" }) ?? [];

    return (
        <Dialog
            open={open}
            onOpenChange={(v) => !update.isPending && onOpenChange(v)}
        >
            <DialogContent className="sm:max-w-[520px]">
                <DialogHeader>
                    <DialogTitle>Update Company</DialogTitle>
                </DialogHeader>

                <form onSubmit={onSubmit} className="space-y-4">
                    {errorMessage && (
                        <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-800">
                            {errorMessage}
                        </div>
                    )}

                    <FormField
                        label="Company Name"
                        error={errors.companyName?.message}
                    >
                        <Input
                            placeholder="Netflix"
                            {...register("companyName")}
                        />
                    </FormField>

                    <FormField
                        label="Careers URL"
                        error={errors.careersUrl?.message}
                    >
                        <Input
                            placeholder="https://jobs.netflix.com"
                            {...register("careersUrl")}
                        />
                    </FormField>

                    <DatePickerField
                        label="Last Visited On (optional)"
                        register={register("lastVisitedOn")}
                        setValue={setValue}
                        value={lastVisitedText}
                        error={errors.lastVisitedOn?.message}
                        helperText="Type MM/DD/YYYY or use the calendar"
                    />

                    <FormField
                        label="Revisit After Days"
                        error={errors.revisitAfterDays?.message}
                        helperText="Allowed: 1 to 20"
                    >
                        <Input
                            type="number"
                            min={1}
                            max={20}
                            {...register("revisitAfterDays", {
                                valueAsNumber: true,
                            })}
                        />
                    </FormField>

                    <TagMultiSelect
                        label="Tags"
                        options={tagOptions}
                        value={selectedTags}
                        onChange={(next) =>
                            setValue("tags", next, { shouldValidate: true })
                        }
                        disabled={update.isPending}
                    />

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
