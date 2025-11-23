import { errorLogger } from '@strudel/core';
import { useSettings, storeStartupScript } from '../../../settings.mjs';
import { SpecialActionInput } from '../button/action-button';

async function importScript(script) {
    const reader = new FileReader()
    reader.readAsText(script)

    reader.onload = () => {
        const text = reader.result;
        console.info(text)
        storeStartupScript(text)

    };

    reader.onerror = () => {
        errorLogger(new Error('failed to import prebake script'), 'ImportPrebakeScriptButton')
    }

}
export function ImportPrebakeScriptButton() {
    const settings = useSettings();

    return <SpecialActionInput type="file"
        label="import prebake script"
        accept=".strudel, .txt"
        onChange={(e) => importScript(e.target.files[0])} />

    // return <label className="hover:opacity-50 cursor-pointer">
    //     <input
    //         style={{ display: 'none' }}
    //         type="file"
    //         // multiple
    //         accept=".strudel, .txt"
    //         onChange={(e) => importScript(e.target.files[0])}
    //     />
    //     import prebake script
    // </label>
}