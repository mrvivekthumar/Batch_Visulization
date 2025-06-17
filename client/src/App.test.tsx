import { render, screen } from '@testing-library/react';
import App from './App';

test('renders performance analyzer', () => {
  render(<App />);
  const titleElement = screen.getByText(/Database Batch Performance Analyzer/i);
  expect(titleElement).toBeInTheDocument();
});