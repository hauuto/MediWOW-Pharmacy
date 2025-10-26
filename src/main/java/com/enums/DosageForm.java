package com.enums;

/**
 * Cột form trong DB có CHECK ('SOLID','LIQUID_DOSAGE','LIQUID_ORAL_DOSAGE')
 * -> Enum này bám DB; BUS sẽ map từ chuỗi UI như "Viên nén", "Si rô"...
 */
public enum DosageForm {
    SOLID, LIQUID_DOSAGE, LIQUID_ORAL_DOSAGE
}
