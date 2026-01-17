import { useEffect, useMemo, useRef } from "react";
import { z } from "zod";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { parseMMddyyyyToYmd } from "@/lib/dateUtils";

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

import { useCreateCompany } from "./hooks";
import { TagMultiSelect } from "./TagMultiSelect";
import { useTagOptions } from "./useTagOptions";

const schema = z.object({
    companyName: z.string().min(1, "Company Name is required").max(120),
    careersUrl: z.string().url("Enter a valid URL"),
    lastVisitedOn: z.string().optional(),
    revisitAfterDays: z.number().int().min(1, "Min 1").max(20, "Max 20"),
    tags: z
        .array(
            z.object({
                key: z.string().min(1),
                label: z.string().min(1),
            })
        )
        .optional()
        .catch([]),
});

type FormValues = z.infer<typeof schema>;

type Properties = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    initialName?: string;
};

export function AddCompanyDialog({
    open,
    onOpenChange,
    initialName,
}: Properties) {
    const create = useCreateCompany();
    const tagOptions = useTagOptions();

    const defaultValues: FormValues = useMemo(
        () => ({
            companyName: initialName ?? "",
            careersUrl: "",
            lastVisitedOn: "",
            revisitAfterDays: 7,
            tags: [],
        }),
        [initialName]
    );

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
        const lastVisistedYmd = parseMMddyyyyToYmd(values.lastVisitedOn ?? "");

        if ((values.lastVisitedOn ?? "").trim() && !lastVisistedYmd) {
            form.setError("lastVisitedOn", {
                type: "manual",
                message: "Use MM/DD/YYYY or pick from calendar",
            });
            return;
        }

        await create.mutateAsync({
            companyName: values.companyName.trim(),
            careersUrl: values.careersUrl.trim(),
            lastVisitedOn: lastVisistedYmd,
            revisitAfterDays: values.revisitAfterDays,
            tags: (values.tags ?? []).map((tag) => ({
                tagKey: tag.key,
                tagName: tag.label,
            })),
        });

        onOpenChange(false);
    });

    const selectedTags = useWatch({ control, name: "tags" }) ?? [];

    return (
        <Dialog
            open={open}
            onOpenChange={(v) => !create.isPending && onOpenChange(v)}
        >
            <DialogContent className="sm:max-w-[520px]">
                <DialogHeader>
                    <DialogTitle>Add Company</DialogTitle>
                </DialogHeader>

                <form onSubmit={onSubmit} className="space-y-4">
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
                        helperText="Type MM/DD/YYYY or use the calendar."
                    />

                    <FormField
                        label="Revisit After (days)"
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
                        disabled={create.isPending}
                    />

                    <div className="flex items-center justify-end gap-2 pt-2">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => onOpenChange(false)}
                            disabled={create.isPending}
                        >
                            Cancel
                        </Button>
                        <Button type="submit" disabled={create.isPending}>
                            {create.isPending ? "Adding..." : "Add"}
                        </Button>
                    </div>
                </form>
            </DialogContent>
        </Dialog>
    );
}
