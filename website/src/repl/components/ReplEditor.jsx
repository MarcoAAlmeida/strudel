import Loader from '@src/repl/components/Loader';
import { HorizontalPanel, VerticalPanel, PanelToggle } from '@src/repl/components/panel/Panel';
import { Code } from '@src/repl/components/Code';
import UserFacingErrorMessage from '@src/repl/components/UserFacingErrorMessage';
import { Footer, Header } from './Header';
import { useSettings } from '@src/settings.mjs';

// type Props = {
//  context: replcontext,
// }

export default function ReplEditor(Props) {
  const { context, ...editorProps } = Props;
  const { containerRef, editorRef, error, init, pending } = context;
  const settings = useSettings();
  const { panelPosition, isZen, isButtonRowHidden } = settings;
  const isEmbedded = typeof window !== 'undefined' && window.location !== window.parent.location;

  return (
    <div className="h-full flex flex-col relative" {...editorProps}>
      <Loader active={pending} />
      <div className="grow flex justify-stretch w-full relative overflow-hidden">
        <div className="flex flex-col grow overflow-hidden">
          <Header context={context} isEmbedded={isEmbedded} />
          <Code containerRef={containerRef} editorRef={editorRef} init={init} />
          {/* <Footer context={context} isEmbedded={isEmbedded} /> */}
        </div>
        {!isZen && panelPosition === 'right' && <VerticalPanel context={context} />}
      </div>
      {!isButtonRowHidden && <Footer context={context} isEmbedded={isEmbedded} />}
      <UserFacingErrorMessage error={error} />
      {!isZen && panelPosition === 'bottom' && <HorizontalPanel context={context} />}
    </div>
  );
}
