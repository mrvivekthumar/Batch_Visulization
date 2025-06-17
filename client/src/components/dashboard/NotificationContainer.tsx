import React, { useState, useEffect } from 'react';
import { Notification, NotificationType } from '../../types';

interface NotificationContainerProps {
    onAddNotification: (addFn: (message: string, type: NotificationType) => void) => void;
}

const NotificationContainer: React.FC<NotificationContainerProps> = ({ onAddNotification }) => {
    const [notifications, setNotifications] = useState<Notification[]>([]);

    useEffect(() => {
        const addNotification = (message: string, type: NotificationType) => {
            const notification: Notification = {
                id: Date.now().toString(),
                message,
                type,
                timestamp: Date.now()
            };

            setNotifications(prev => [...prev, notification]);

            // Auto-remove after 5 seconds
            setTimeout(() => {
                setNotifications(prev => prev.filter(n => n.id !== notification.id));
            }, 5000);
        };

        onAddNotification(addNotification);
    }, [onAddNotification]);

    const removeNotification = (id: string) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    return (
        <div className="notification-container">
            {notifications.map(notification => (
                <div
                    key={notification.id}
                    className={`notification notification-${notification.type} show`}
                    onClick={() => removeNotification(notification.id)}
                >
                    {notification.message}
                </div>
            ))}
        </div>
    );
};

export default NotificationContainer;