// Simple solution - no web vitals errors
const reportWebVitals = (onPerfEntry?: any) => {
  // Web vitals disabled for now - can be enabled later if needed
  if (process.env.NODE_ENV === 'development') {
    console.log('Web vitals reporting is disabled');
  }
};

export default reportWebVitals;