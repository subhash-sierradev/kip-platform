import { AlertTriangle, CheckCircle, Info, XCircle } from 'lucide-vue-next';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
  timestamp: number;
}

// Icon mapping for toast types
export const getToastIcon = (type: ToastType) => {
  const icons = {
    error: XCircle,
    warning: AlertTriangle,
    success: CheckCircle,
    info: Info,
  };
  return icons[type];
};

// Simple utility for immediate alerts (fallback when toast system not available)
const showBrowserAlert = (message: string, type: ToastType) => {
  const prefix =
    type === 'error'
      ? '[ERROR] '
      : type === 'warning'
        ? '[WARNING] '
        : type === 'success'
          ? '[SUCCESS] '
          : '[INFO] ';
  console.warn(prefix + message);
};

let globalToastFunction: ((message: string, type: ToastType) => void) | null = null;

export const setGlobalToast = (toastFn: (message: string, type: ToastType) => void) => {
  globalToastFunction = toastFn;
};

class Alert {
  static warning(message: string) {
    if (globalToastFunction) {
      globalToastFunction(message, 'warning');
    } else {
      showBrowserAlert(message, 'warning');
    }
  }

  static success(message: string) {
    if (globalToastFunction) {
      globalToastFunction(message, 'success');
    } else {
      showBrowserAlert(message, 'success');
    }
  }

  static error(message: string) {
    if (globalToastFunction) {
      globalToastFunction(message, 'error');
    } else {
      showBrowserAlert(message, 'error');
    }
  }

  static info(message: string) {
    if (globalToastFunction) {
      globalToastFunction(message, 'info');
    } else {
      showBrowserAlert(message, 'info');
    }
  }
}

export default Alert;
