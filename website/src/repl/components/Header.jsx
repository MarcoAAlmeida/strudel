import PlayCircleIcon from '@heroicons/react/20/solid/PlayCircleIcon';
import StopCircleIcon from '@heroicons/react/20/solid/StopCircleIcon';
import cx from '@src/cx.mjs';
import { useSettings, setIsZen, setIsPanelOpened } from '../../settings.mjs';
import { StrudelIcon } from '@src/repl/components/icons/StrudelIcon';
import '../Repl.css';
import { Bars3Icon } from '@heroicons/react/16/solid';

const { BASE_URL } = import.meta.env;
const baseNoTrailing = BASE_URL.endsWith('/') ? BASE_URL.slice(0, -1) : BASE_URL;

export function Header({ context, embedded = false }) {
  const { started, pending, isDirty, activeCode, handleTogglePlay, handleEvaluate, handleShuffle, handleShare } =
    context;
  const isEmbedded = typeof window !== 'undefined' && (embedded || window.location !== window.parent.location);
  const { isZen, isButtonRowHidden, isCSSAnimationDisabled, fontFamily, isPanelOpen } = useSettings();

  return (
    <header
      id="header"
      className={cx(
        'flex-none text-black  z-[100] text-sm select-none min-h-10',
        !isZen && !isEmbedded && 'bg-lineHighlight',
        isZen ? 'h-12 w-8 fixed top-0 left-0' : 'sticky top-0 w-full justify-between',
        isEmbedded ? 'flex' : 'sm:flex',
      )}
      style={{ fontFamily }}
    >
      <div className="px-4 pt-1 flex space-x-2 md:pt-0 select-none">
        <h1
          onClick={() => {
            if (isEmbedded) window.open(window.location.href.replace('embed', ''));
          }}
          className={cx(
            isEmbedded ? 'text-l cursor-pointer' : 'text-xl',
            'text-foreground font-bold flex space-x-2 items-center',
          )}
        >
          <div
            className={cx(
              'mt-[1px]',
              started && !isCSSAnimationDisabled && 'animate-spin',
              'cursor-pointer text-blue-500',
              isZen && 'fixed top-2 right-4',
            )}
            onClick={() => {
              if (!isEmbedded) {
                setIsZen(!isZen);
              }
            }}
          >
            <span className="block text-foreground rotate-90">
              <StrudelIcon className="w-5 h-5 fill-foreground" />
            </span>
          </div>
          {!isZen && (
            <div className="space-x-2">
              <span className="">strudel</span>
              <span className="text-sm font-medium">REPL</span>
              {!isEmbedded && isButtonRowHidden && (
                <a href={`${baseNoTrailing}/learn`} className="text-sm opacity-25 font-medium">
                  DOCS
                </a>
              )}
            </div>
          )}
        </h1>
      </div>
      {!isZen && !isButtonRowHidden && (
        <div className="flex max-w-full overflow-auto text-foreground px-2 h-10 justify-end">
          <button
            onClick={handleTogglePlay}
            title={started ? 'stop' : 'play'}
            className={cx('px-2 hover:opacity-50', !started && !isCSSAnimationDisabled && 'animate-pulse')}
          >
            {!pending ? (
              <span className={cx('flex items-center space-x-2')}>
                {started ? <StopCircleIcon className="w-5 h-5" /> : <PlayCircleIcon className="w-5 h-5" />}
                {!isEmbedded && <span>{started ? 'stop' : 'play'}</span>}
              </span>
            ) : (
              <>loading...</>
            )}
          </button>
          <button
            onClick={handleEvaluate}
            title="update"
            className={cx(
              'flex items-center space-x-1 px-2',
              !isDirty || !activeCode ? 'opacity-50' : 'hover:opacity-50',
            )}
          >
            {!isEmbedded && <span>update</span>}
          </button>
          {!isEmbedded && (
            <button
              title="share"
              className={cx('cursor-pointer hover:opacity-50 flex items-center space-x-1 px-2')}
              onClick={handleShare}
            >
              <span>share</span>
            </button>
          )}
          {!isEmbedded && (
            <a
              title="learn"
              href={`${baseNoTrailing}/workshop/getting-started/`}
              className={cx('hover:opacity-50 flex items-center space-x-1', !isEmbedded ? 'p-2' : 'px-2')}
            >
              <span>learn</span>
            </a>
          )}
          {!isEmbedded && (
            <button
              title="share"
              className={cx('cursor-pointer hover:opacity-50 flex items-center space-x-1 pl-2 pr-1')}
              onClick={() => setIsPanelOpened(!isPanelOpen)}
            >
              <span>menu</span>
              <Bars3Icon className="w-5 h-5" />
            </button>
          )}
        </div>
      )}
    </header>
  );
}
