// Shared jsdom <dialog> shim (S5), wired via `test.options.setupFiles` in
// angular.json. jsdom implements the <dialog> element but not its modal
// semantics (showModal/close are missing); the always-rendered dialogs (F7)
// need them. The `typeof` guards make the shim idempotent and order-safe:
// it only fills in missing methods and never overrides existing ones.
if (typeof HTMLDialogElement.prototype.showModal !== 'function') {
  HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
    this.setAttribute('open', '');
  };
}
if (typeof HTMLDialogElement.prototype.close !== 'function') {
  HTMLDialogElement.prototype.close = function (this: HTMLDialogElement) {
    this.removeAttribute('open');
  };
}
