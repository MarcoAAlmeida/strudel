import cx from '@src/cx.mjs';

export function ActionButton({ children, label, labelIsHidden, className, ...buttonProps }) {
  return (
    <button className={cx('hover:opacity-50 text-nowrap w-fit', className)} title={label} {...buttonProps}>
      {labelIsHidden !== true && label}
      {children}
    </button>
  );
}
export function ActionInput({ label, className, ...inputProps }) {
  return (
    <label className={cx("hover:opacity-50 cursor-pointer text-nowrap w-fit", className)}>
      <input
        style={{ display: 'none' }}
        {...inputProps}
      />
      {label}
    </label>
  );
}

export function SpecialActionButton(props) {
  const { className, ...buttonProps } = props

  return <ActionButton {...buttonProps} className={cx("bg-background p-2 max-w-[300px] rounded-md hover:opacity-50", className)} />

}
export function SpecialActionInput(props) {
  const { className, ...inputProps } = props
  return <ActionInput {...inputProps} className={cx("bg-background p-2  max-w-[300px] rounded-md hover:opacity-50", className)} />
}


