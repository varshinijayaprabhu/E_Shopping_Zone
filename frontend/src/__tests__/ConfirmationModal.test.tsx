import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ConfirmationModal } from '../components/common/ConfirmationModal';

describe('ConfirmationModal Component', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <ConfirmationModal
        isOpen={false}
        title="Test Modal"
        message="Test Message"
        onConfirm={() => {}}
        onCancel={() => {}}
      />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders title, message and triggers callbacks', () => {
    const handleConfirm = vi.fn();
    const handleCancel = vi.fn();

    render(
      <ConfirmationModal
        isOpen={true}
        title="Are you sure?"
        message="This is a permanent operation"
        confirmText="Yes, Proceed"
        cancelText="No, Abort"
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    );

    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
    expect(screen.getByText('This is a permanent operation')).toBeInTheDocument();

    const confirmBtn = screen.getByText('Yes, Proceed');
    fireEvent.click(confirmBtn);
    expect(handleConfirm).toHaveBeenCalledTimes(1);

    const cancelBtn = screen.getByText('No, Abort');
    fireEvent.click(cancelBtn);
    expect(handleCancel).toHaveBeenCalledTimes(1);
  });
});

