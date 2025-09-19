import * as XLSX from 'xlsx';

export interface ExcelHeader {
    column: string;
    value: string;
    index: number;
}

export interface ExcelPreview {
    headers: ExcelHeader[];
    hasData: boolean;
    rowCount: number;
}

/**
 * Parse Excel file and extract headers for mapping
 */
export async function parseExcelHeaders(file: File): Promise<ExcelPreview> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        
        reader.onload = (e) => {
            try {
                const data = new Uint8Array(e.target?.result as ArrayBuffer);
                const workbook = XLSX.read(data, { type: 'array' });
                
                // Get first sheet
                const sheetName = workbook.SheetNames[0];
                const worksheet = workbook.Sheets[sheetName];
                
                // Get range
                const range = XLSX.utils.decode_range(worksheet['!ref'] || 'A1:A1');
                
                const headers: ExcelHeader[] = [];
                
                // Extract first row as headers
                for (let col = range.s.c; col <= range.e.c; col++) {
                    const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
                    const cell = worksheet[cellAddress];
                    const columnLetter = XLSX.utils.encode_col(col);
                    
                    headers.push({
                        column: columnLetter,
                        value: cell ? String(cell.v) : `Колонка ${columnLetter}`,
                        index: col
                    });
                }
                
                resolve({
                    headers,
                    hasData: range.e.r > 0,
                    rowCount: range.e.r + 1
                });
                
            } catch (error) {
                reject(new Error('Ошибка чтения Excel файла: ' + error));
            }
        };
        
        reader.onerror = () => {
            reject(new Error('Ошибка чтения файла'));
        };
        
        reader.readAsArrayBuffer(file);
    });
}

/**
 * Validate Excel file before processing
 */
export function validateExcelFile(file: File): string | null {
    if (!file) {
        return 'Файл не выбран';
    }
    
    if (file.size > 10 * 1024 * 1024) { // 10MB
        return 'Размер файла превышает 10MB';
    }
    
    const filename = file.name.toLowerCase();
    if (!filename.endsWith('.xlsx') && !filename.endsWith('.xls')) {
        return 'Поддерживаются только файлы Excel (.xlsx, .xls)';
    }
    
    if (filename.endsWith('.xlsm')) {
        return 'Файлы с макросами не поддерживаются по соображениям безопасности';
    }
    
    return null;
}
