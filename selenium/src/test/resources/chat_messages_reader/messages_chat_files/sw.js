/* eslint-env browser, serviceworker, es6 */

'use strict';

self.addEventListener('push', function(event) {
    let response = JSON.parse(event.data.text());
    let data     = JSON.parse(response.data);
    console.log(response);
    console.log(JSON.parse(response.data));

    const title = data.title;
    const options = {
        body: data.body,
        icon: data.icon,
        image: data.image,
        badge: data.badge,
        vibrate: data.vibrate,
        requireInteraction: data.requireInteraction,
        data: {
          link: data.link,
        }
    };

    const notificationPromise = self.registration.showNotification(title, options);
    event.waitUntil(notificationPromise);
});

self.addEventListener('notificationclick', function(event) {
    event.waitUntil(
        clients.openWindow(event.notification.data.link)
    );

    navigator.vibrate(event.notification.vibrate);

    event.notification.close();
});
