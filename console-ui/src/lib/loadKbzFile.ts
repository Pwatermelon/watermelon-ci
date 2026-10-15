/** Read a local .kbz / text file into a string (UTF-8). */
export function readLocalTextFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const text = typeof reader.result === 'string' ? reader.result : ''
      resolve(text.replace(/^\uFEFF/, ''))
    }
    reader.onerror = () => reject(reader.error || new Error('не удалось прочитать файл'))
    reader.readAsText(file, 'UTF-8')
  })
}

export const KBZ_FILE_ACCEPT = '.kbz,.txt,text/plain'
