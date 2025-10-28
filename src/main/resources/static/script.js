// Global variable to store authentication token
let token = '';

/**
 * Handles user login process
 * @param {Event} event - The form submission event
 */
async function login(event) {
    // Prevent default form submission behavior
    event.preventDefault();
    // Get username and password from form inputs
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        // Log login attempt
        console.log('Attempting login for user:', username);
        // Send login request to server
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        // Parse response data
        const data = await response.json();
        
        // Handle successful login
        if (response.ok) {
            // Store token in localStorage
            localStorage.setItem('token', data.token);
            // Redirect to dashboard
            window.location.href = 'dashboard.html';
        } else {
            // Display error message if login fails
            alert(data.message || 'Login failed');
        }
    } catch (error) {
        // Log and display any errors during login process
        console.error('Login error:', error);
        alert('Login failed: ' + error.message);
    }
}

/**
 * Checks if the stored authentication token has expired
 * Redirects to login page if token is expired
 */
function checkTokenExpiration() {
    // Get token from localStorage
    const token = localStorage.getItem('token');
    if (token) {
        try {
            // Decode token payload
            const payload = JSON.parse(atob(token.split('.')[1]));
            // Check if token has expired
            if (payload.exp * 1000 < Date.now()) {
                // Remove expired token
                localStorage.removeItem('token');
                // Redirect to login page
                window.location.href = '/';
            }
        } catch (e) {
            // Log any errors during token validation
            console.error('Error checking token:', e);
        }
    }
}

// Check token expiration when page loads
document.addEventListener('DOMContentLoaded', checkTokenExpiration);
