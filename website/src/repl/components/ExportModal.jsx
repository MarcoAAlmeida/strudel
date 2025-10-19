import PlayCircleIcon from '@heroicons/react/20/solid/PlayCircleIcon';
import cx from '@src/cx.mjs';
import NumberInput from './NumberInput';
import { useState } from 'react';
import { Textbox } from './textbox/Textbox';
import { setMultiChannelOrbits as setStrudelMultiChannelOrbits, setMaxPolyphony as setStrudelMaxPolyphony } from '@strudel/webaudio';
function Checkbox({ label, value, onChange, disabled = false }) {
    return (
        <label>
            <input disabled={disabled} type="checkbox" checked={value} onChange={onChange} />
            {' ' + label}
        </label>
    );
}
export default function ExportModal(Props) {
    const { started, isEmbedded, handleExport } = Props;

    const [startCycle, setStartCycle] = useState(0) // TODO: make a form?
    const [endCycle, setEndCycle] = useState(0)
    const [multiChannelOrbits, setMultiChannelOrbits] = useState(true)
    const [maxPolyphony, setMaxPolyphony] = useState(1024)
    const [exporting, setExporting] = useState(false)
    return (
        <>
            <button
                onClick={() => {
                    if (started) return;
                    const modal = document.getElementById("exportModal");
                    modal.showModal();
                }}
                title="export"
                className={cx(
                    'flex items-center space-x-1',
                    !isEmbedded ? 'p-2' : 'px-2',
                    started ? 'opacity-50' : 'hover:opacity-50',
                )}
            >
                {!isEmbedded && <span>export</span>}
            </button>
            <dialog closedby={exporting ? "none" : "closerequest"} id='exportModal' className='bg-background text-foreground'>
                <button autofocus onClick={() => {
                    if (exporting) return;
                    const modal = document.getElementById("exportModal");
                    modal.close();
                }}>Close</button>
                <div className='flex flex-row items-center gap-4'>
                    <p>Start cycle</p>
                    <Textbox
                        min={1}
                        max={Infinity}
                        onBlur={(e) => {
                            let v = parseInt(e.target.value);
                            console.log(v)
                            v = isNaN(v) ? 0 : Math.max(1, v);
                            setStartCycle(v);
                        }}
                        onChange={v => {
                            v = parseInt(v)
                            setStartCycle(v);
                        }}
                        type="number"
                        placeholder=""
                        value={startCycle ?? ''}
                    />
                </div>
                <div className='flex flex-row items-center gap-4'>
                    <p>End cycle</p>
                    <Textbox
                        min={1}
                        max={Infinity}
                        onBlur={(e) => {
                            let v = parseInt(e.target.value);
                            v = isNaN(v) ? Math.max(startCycle + 1, parseInt(v)) : v;
                            setEndCycle(v);
                        }}
                        onChange={(v) => {
                            v = parseInt(v)
                            setEndCycle(v);
                        }}
                        type="number"
                        placeholder=""
                        value={endCycle ?? ''}
                    />
                </div>
                <div className='flex flex-row items-center gap-4'>
                    <p>Maximum polyphony</p>
                    <Textbox
                        min={1}
                        max={Infinity}
                        onBlur={(e) => {
                            let v = parseInt(e.target.value);
                            v = isNaN(v) ? Math.max(1, parseInt(v)) : v;
                            setMaxPolyphony(v);
                        }}
                        onChange={(v) => {
                            v = Math.max(1, parseInt(v));
                            setMaxPolyphony(v);
                        }}
                        type="number"
                        placeholder=""
                        value={maxPolyphony ?? ''}
                    />
                </div>
                <div>
                    <Checkbox
                        label="Multi Channel Orbits"
                        onChange={(cbEvent) => {
                            const val = cbEvent.target.checked;
                            setMultiChannelOrbits(val)
                        }}
                        value={multiChannelOrbits}
                    />
                </div>
                <button onClick={async () => {
                    setStrudelMaxPolyphony(maxPolyphony)
                    setStrudelMultiChannelOrbits(multiChannelOrbits)
                    setExporting(true)
                    await handleExport(startCycle, endCycle).then(() => {
                        setExporting(false)
                        const modal = document.getElementById("exportModal");
                        modal.close();
                    })
                }}>Export to wav</button>
            </dialog>
        </>
    );
}
