import React, { useMemo } from "react";
import find from "lodash/find";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ColumnData } from "@/types/shared";

export type ColumnSelectorProps<TColumnData> = {
  field: string;
  columns: ColumnData<TColumnData>[];
  onSelect: (column: ColumnData<TColumnData>) => void;
};

const ColumnSelector = <TColumnData,>({
  field,
  columns,
  onSelect,
}: ColumnSelectorProps<TColumnData>) => {
  const selectedColumn = useMemo(() => {
    return find(columns, (c) => c.id === field);
  }, [field, columns]);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button className="w-full" variant="outline">
          {selectedColumn?.label ?? (field || "Column")}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-52">
        {columns.map((column) => (
          <DropdownMenuCheckboxItem
            key={column.id}
            checked={column.id === selectedColumn?.id}
            onSelect={() => onSelect(column)}
            disabled={column.disabled}
          >
            {column.label}
          </DropdownMenuCheckboxItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default ColumnSelector;