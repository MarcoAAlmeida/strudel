import { errorLogger } from '@strudel/core';
import { useSettings, storeStartupScript } from '../../../settings.mjs';
import { SpecialActionInput } from '../button/action-button';

async function importScript(script) {
  const reader = new FileReader();
  reader.readAsText(script);

  reader.onload = () => {
    const text = reader.result;
    storeStartupScript(text);
  };

  reader.onerror = () => {
    errorLogger(new Error('failed to import prebake script'), 'importScript');
  };
}
export function ImportPrebakeScriptButton() {
  const settings = useSettings();

  return (
    <SpecialActionInput
      type="file"
      label="import prebake script"
      accept=".strudel"
      onChange={(e) => importScript(e.target.files[0])}
    />
  );
}
