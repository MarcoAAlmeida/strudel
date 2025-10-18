/*
stateful.mjs - File of shame for stateful, impure and otherwise illegal pattern methods
Copyright (C) 2025 Strudel contributors - see <https://codeberg.org/uzu/strudel/src/branch/main/packages/core/index.mjs>
This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details. You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import { register, reify, Pattern } from './pattern.mjs';

let timelines = {};

export const reset_timelines = function () {
  timelines = {};
};

export const timeline = register(
  'timeline',
  function (tpat, pat) {
    tpat = reify(tpat);
    const f = function (state) {
      let scheduler = !!state.controls._cps;

      const timehaps = tpat.query(state);
      const result = [];
      for (const timehap of timehaps) {
        const tlid = timehap.value;
        const ignore = false;
        let offset;
        if (tlid in timelines) {
          offset = timelines[tlid];
        } else {
          const timearc = timehap.wholeOrPart();
          if (!scheduler || state.span.begin.lt(timearc.midpoint())) {
            offset = timearc.begin;
          } else {
            // Sync to end of timearc if we first see it over halfway into its
            // timespan. Allows 'cuing up' next timeline when live coding.
            offset = timearc.end;
          }
        }
        if (scheduler) {
          // update state
          timelines[tlid] = offset;
          const negative = 0 - tlid;
          if (negative in timelines) {
            delete timelines[negative];
          }
        }

        const pathaps = pat
          .late(offset)
          .query(state.setSpan(timehap.part))
          .map((h) => h.setContext(h.combineContext(timehap)));
        result.push(...pathaps);
      }
      return result;
    };
    return new Pattern(f, pat._steps);
  },
  false,
);
