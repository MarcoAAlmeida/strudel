import { useMemo, useState } from 'react';

import jsdocJson from '../../../../../doc.json';
import { Textbox } from '../textbox/Textbox';

const isValid = ({ name, description }) => name && !name.startsWith('_') && !!description;

const availableFunctions = (() => {
  const seen = new Set(); // avoid repetition
  const functions = [];
  for (const doc of jsdocJson.docs) {
    if (!isValid(doc)) continue;
    functions.push(doc);
    const synonyms = doc.synonyms || [];
    for (const s of synonyms) {
      if (!s || seen.has(s)) continue;
      seen.add(s);
      // Swap `doc.name` in for `s` in the list of synonyms
      const synonymsWithDoc = [doc.name, ...synonyms].filter((x) => x && x !== s);
      // functions.push({
      //   ...doc,
      //   name: s, // update names for the synonym
      //   longname: s,
      //   synonyms: synonymsWithDoc,
      //   synonyms_text: synonymsWithDoc.join(', '),
      // });
    }
  }
  return functions.sort((a, b) => /* a.meta.filename.localeCompare(b.meta.filename) +  */ a.name.localeCompare(b.name));
})();

const getInnerText = (html) => {
  var div = document.createElement('div');
  div.innerHTML = html;
  return div.textContent || div.innerText || '';
};

const GROUP_DISPLAY_NAMES = {
  external_io: 'External I/O',
  effects: 'Effects',
  ungrouped: 'Ungrouped',
};

const GROUP_ORDER = ['effects', 'external_io', 'ungrouped'];

export function Reference() {
  const [search, setSearch] = useState('');

  const visibleFunctions = useMemo(() => {
    return availableFunctions.filter((entry) => {
      if (!search) {
        return true;
      }

      const lowCaseSearch = search.toLowerCase();
      return (
        entry.name.toLowerCase().includes(lowCaseSearch) ||
        (entry.synonyms?.some((s) => s.includes(lowCaseSearch)) ?? false)
      );
    });
  }, [search]);

  const visibleFunctionsByGroup = (() => {
    const groups = {};
    for (const doc of visibleFunctions) {
      const group = doc.group || 'ungrouped';
      if (!groups[group]) {
        groups[group] = [];
      }
      groups[group].push(doc);
    }
    return groups;
  })();
  // console.log(visibleFunctionsByGroup);

  // Sort and map group entries
  const sortedGroups = Object.entries(visibleFunctionsByGroup).sort(([a], [b]) => {
    const ai = GROUP_ORDER.indexOf(a);
    const bi = GROUP_ORDER.indexOf(b);
    if (ai === -1 && bi === -1) return a.localeCompare(b);
    if (ai === -1) return 1;
    if (bi === -1) return -1;
    return ai - bi;
  });

  return (
    <div className="flex h-full w-full p-2 overflow-hidden">
      <div className="h-full  flex flex-col gap-2 w-1/3 max-w-72 ">
        <div class="w-full flex">
          <Textbox className="w-full" placeholder="Search" value={search} onChange={setSearch} />
        </div>
        <div className="flex flex-col h-full overflow-y-auto  gap-1.5 bg-background bg-opacity-50  rounded-md">
          {sortedGroups.map(([groupId, groupEntries]) => (
            <>
              <h4 key={`group-${groupId}`} className="font-semibold text-foreground">
                {GROUP_DISPLAY_NAMES[groupId] || groupId}
              </h4>
              {groupEntries.map((entry, i) => (
                <a
                  key={`group-${groupId}-entry-${i}`}
                  className="cursor-pointer text-foreground flex-none hover:bg-lineHighlight overflow-x-hidden  px-1 text-ellipsis"
                  onClick={() => {
                    const el = document.getElementById(`doc-${entry.name}`);
                    const container = document.getElementById('reference-container');
                    container.scrollTo(0, el.offsetTop);
                  }}
                >
                  {entry.name} {/* <span className="text-gray-600">{entry.meta.filename}</span> */}
                </a>
              ))}
            </>
          ))}
        </div>
      </div>
      <div
        className="break-normal flex-grow flex-col overflow-y-auto overflow-x-hidden   px-2 flex relative"
        id="reference-container"
      >
        <div className="prose dark:prose-invert min-w-full px-1 ">
          <h2>API Reference</h2>
          <p>
            This is the long list of functions you can use. Remember that you don't need to remember all of those and
            that you can already make music with a small set of functions!
          </p>
          {visibleFunctions.map((entry, i) => (
            <section key={i}>
              <h3 id={`doc-${entry.name}`}>{entry.name}</h3>
              {!!entry.synonyms_text && (
                <p>
                  Synonyms: <code>{entry.synonyms_text}</code>
                </p>
              )}
              {/* <small>{entry.meta.filename}</small> */}
              <p dangerouslySetInnerHTML={{ __html: entry.description }}></p>
              <ul>
                {entry.params?.map(({ name, type, description }, i) => (
                  <li key={i}>
                    {name} : {type?.names?.join(' | ')} {description ? <> - {getInnerText(description)}</> : ''}
                  </li>
                ))}
              </ul>
              {entry.examples?.map((example, j) => (
                <pre className="bg-background" key={j}>
                  {example}
                </pre>
              ))}
            </section>
          ))}
        </div>
      </div>
    </div>
  );
}
