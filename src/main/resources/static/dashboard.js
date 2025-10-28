// Add at the top, right after any imports
const token = localStorage.getItem('token');
if (!token) {
    localStorage.clear();
    window.location.href = '/';
}

// Use the existing token variable throughout the file
const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
};

// Load subjects when page loads
async function loadSubjects() {
    // This function won't work as there's no /api/subjects endpoint
}

// Call immediately and after DOM loads
loadSubjects();
document.addEventListener('DOMContentLoaded', loadSubjects);

// Define functions first
function displayQuestionBanks(banks) {
    const banksList = document.getElementById('banksList');
    banksList.innerHTML = banks.length ? 
        banks.map(bank => `
            <div class="bank-item">
                <h3>${bank.title}</h3>
                <p>${bank.description || ''}</p>
                <button onclick="window.location.href='questionbank.html?id=${bank.id}'">Open Bank</button>
            </div>
        `).join('') : 
        '<p>No question banks found</p>';
}

async function loadQuestionBanks() {
    try {
        const response = await fetch('/api/questionbanks', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                localStorage.clear();
                window.location.href = '/';
                return;
            }
            throw new Error('Failed to load question banks');
        }

        const banks = await response.json();
        displayBanks(banks);
    } catch (error) {
        console.error('Error:', error);
        // Only clear token on auth errors
        if (error.message.includes('401') || error.message.includes('403')) {
            localStorage.clear();
            window.location.href = '/';
        } else {
            console.error('Error loading question banks:', error.message);
        }
    }
}

// Add this at the top with other global variables
let currentBanks = [];

function displayBanks(banks) {
    currentBanks = banks;
    const banksList = document.getElementById('banksList');
    banksList.innerHTML = banks.map(bank => `
        <div class="bank-card" onclick="handleBankClick(event, ${bank.id})">
            <div class="bank-header">
                <h3>${bank.title}</h3>
                <div class="bank-actions">
                    <button data-action="publish" class="publish-btn ${bank.published ? 'published' : ''}">
                        ${bank.published ? 'Unpublish' : 'Publish'}
                    </button>
                    <button data-action="edit" class="edit-btn">Edit</button>
                    <button data-action="delete" class="delete-btn">Delete</button>
                </div>
            </div>
            ${bank.description ? `
                <div class="bank-description">
                    ${bank.description}
                </div>
            ` : ''}
        </div>
    `).join('');
}

function handleBankClick(event, bankId) {
    const target = event.target;
    
    // Check if a button was clicked
    if (target.matches('button')) {
        event.stopPropagation();
        
        // Handle different button actions
        switch (target.dataset.action) {
            case 'edit':
                editBank(bankId);
                break;
            case 'publish':
                togglePublish(bankId);
                break;
            case 'delete':
                deleteBank(bankId);
                break;
        }
    } else {
        // If not a button click, open the question bank
        openQuestionBank(bankId);
    }
}

function editBank(bankId) {
    const bank = currentBanks.find(b => b.id === bankId);
    if (!bank) return;

    const title = prompt('Enter new title:', bank.title);
    if (!title) return;

    const description = prompt('Enter new description:', bank.description || '');

    updateBank(bankId, {
        title: title,
        description: description || null // Set to null if empty string
    });
}

async function updateBank(bankId, data) {
    try {
        // Find the current bank to preserve its published status
        const currentBank = currentBanks.find(b => b.id === bankId);
        if (!currentBank) return;

        // Include the published status in the update data
        const updateData = {
            ...data,
            published: currentBank.published // Preserve the published status
        };

        const response = await fetch(`/api/questionbanks/${bankId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(updateData)
        });

        if (response.ok) {
            // Update the bank in currentBanks array
            const updatedBank = await response.json();
            const bankIndex = currentBanks.findIndex(b => b.id === bankId);
            if (bankIndex !== -1) {
                currentBanks[bankIndex] = updatedBank;
                displayBanks(currentBanks);
            }
        } else {
            alert('Failed to update bank');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error updating bank');
    }
}

// Then add event listeners
document.addEventListener('DOMContentLoaded', () => {
    loadQuestionBanks();
    setupLogout();
});

// Create bank form submission
document.getElementById('bankForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const bankData = {
        title: document.getElementById('bankTitle').value,
        description: document.getElementById('bankDescription').value
    };

    try {
        const response = await fetch('/api/questionbanks', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify(bankData)
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to create bank');
        }

        const data = await response.json();
        console.log('Bank created:', data);
        await loadQuestionBanks();  // Reload the banks list
        e.target.reset();
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to create bank: ' + error.message);
    }
});

function openQuestionBank(bankId) {
    window.location.href = `questionbank.html?id=${bankId}`;
}

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadSubjects();
    loadQuestionBanks();

    // Add logout handler
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }
});

// Add subject management
async function addSubject(subjectName) {
    try {
        const response = await fetch('/api/subjects/create', {
            method: 'POST',
            headers: {
                ...headers,
                'Content-Type': 'text/plain'
            },
            body: subjectName
        });
        
        if (response.ok) {
            loadSubjects(); // Refresh subject lists
            return true;
        }
        return false;
    } catch (error) {
        console.error('Error adding subject:', error);
        return false;
    }

}

// Add this function to handle logout
function logout() {
    localStorage.removeItem('token');
    window.location.href = '/';
}

// Make sure this function is called when page loads
document.addEventListener('DOMContentLoaded', loadQuestionBanks); 

// Add this function for better error notifications
function showNotification(message, type = 'error') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}
// Add loading states to forms
async function handleFormSubmit(e, loadingButton) {
    const originalText = loadingButton.textContent;
    loadingButton.disabled = true;
    loadingButton.textContent = 'Loading...';
    
    try {
        // your async operation
    } finally {
        loadingButton.disabled = false;
        loadingButton.textContent = originalText;
    }
} 

async function deleteBank(bankId) {
    if (!confirm('Are you sure you want to delete this bank? This action cannot be undone.')) {
        return;
    }

    try {
        const response = await fetch(`/api/questionbanks/${bankId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            loadQuestionBanks(); // Refresh the list
        } else {
            alert('Failed to delete bank');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error deleting bank');
    }
}

function handleSessionExpired() {
    localStorage.clear();
    window.location.href = '/';
}

// Update the alert message to include a button that calls handleSessionExpired
function showSessionExpiredAlert() {
    const message = 'Session expired. Please login again.';
    alert(message);
    handleSessionExpired();
}

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch('/api/quizzes/my-results', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to fetch quiz results');
        }

        const quizzes = await response.json();
        displayQuizzes(quizzes);

    } catch (error) {
        console.error('Error:', error);
        alert('Failed to load quiz results: ' + error.message);
    }
});

function calculateTimeSpent(startTime, endTime) {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diff = Math.floor((end - start) / 1000); // difference in seconds
    
    const minutes = Math.floor(diff / 60);
    const seconds = diff % 60;
    
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

// Add error handling for token expiration
function handleSessionExpired() {
    localStorage.clear();
    const message = 'Session expired. Please login again.';
    alert(message);
    window.location.href = '/';
}

function deleteQuiz(event, quizId) {
    event.stopPropagation(); // Prevent triggering the parent click
    
    if (confirm('Are you sure you want to delete this quiz?')) {
        fetch(`/api/quizzes/${quizId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        })
        .then(response => {
            if (response.status === 404) {
                throw new Error('Quiz not found');
            }
            if (response.status === 403) {
                throw new Error('Not authorized to delete this quiz');
            }
            if (!response.ok) {
                throw new Error('Failed to delete quiz');
            }
            
            // Remove the quiz item from the DOM
            const quizItem = event.target.closest('.quiz-item');
            if (quizItem) {
                quizItem.remove();
            }
            
            // Optional: Refresh the quiz list
            loadQuizzes();
        })
        .catch(error => {
            console.error('Error:', error);
            // Don't show the error alert if the quiz was actually deleted
            if (error.message !== 'Failed to load quizzes') {
                alert('Error: ' + error.message);
            }
        });
    }
}

function loadQuizzes() {
    fetch('/api/quizzes/my-results', {  // Updated endpoint
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Failed to load quizzes');
        }
        return response.json();
    })
    .then(quizzes => {
        displayQuizzes(quizzes);
    })
    .catch(error => {
        console.error('Error:', error);
        // Only show alert for actual loading errors
        if (error.message === 'Failed to load quizzes') {
            alert('Failed to load quizzes. Please refresh the page.');
        }
    });
}

function displayQuizzes(quizzes) {
    const quizList = document.getElementById('quizList');


    quizList.innerHTML = quizzes.map(quiz => {
        // Calculate score status
        let scoreStatus = 'not-graded';
        let scoreColor = '#f1c40f'; // Default yellow for not graded
        
        if (quiz.score !== null && quiz.totalPossibleScore > 0) {
            const scorePercentage = quiz.score / quiz.totalPossibleScore;
            
            if (scorePercentage < 1/3) {
                scoreStatus = 'low';
                scoreColor = '#e74c3c'; // Red
            } else if (scorePercentage < 2/3) {
                scoreStatus = 'medium';
                scoreColor = '#f1c40f'; // Yellow
            } else {
                scoreStatus = 'high';
                scoreColor = '#2ecc71'; // Green
            }
        }

        const scoreDisplay = quiz.score !== null 
            ? `${quiz.score}/${quiz.totalPossibleScore}`
            : 'Not graded yet';

        return `
            <div class="quiz-item" data-score="${scoreStatus}" onclick="viewQuiz(${quiz.id}, event)">
                <div class="quiz-info">
                    <div class="quiz-date">
                        Quiz taken on ${new Date(quiz.startTime).toLocaleString()}
                    </div>
                    <div class="quiz-stats">
                        <div class="quiz-stat">
                            <span class="quiz-stat-label">Score:</span>
                            <span class="quiz-stat-value" style="color: ${scoreColor}">${scoreDisplay}</span>
                        </div>
                        <div class="quiz-stat">
                            <span class="quiz-stat-label">Time spent:</span>
                            <span class="quiz-stat-value">${calculateTimeSpent(quiz.startTime, quiz.endTime)}</span>
                        </div>
                    </div>
                </div>
                <button onclick="deleteQuiz(event, ${quiz.id})" class="delete-quiz-btn">Delete Quiz</button>
            </div>
        `;
    }).join('');
}

function viewQuiz(quizId, event) {
    // Prevent triggering if clicking delete button
    if (event.target.classList.contains('delete-quiz-btn')) {
        return;
    }
    window.location.href = `/quiz-results.html?quizId=${quizId}`;
}

function displayTopics(topics) {
    const topicsList = document.getElementById('topicsList');
    if (!topics || topics.length === 0) {
        topicsList.innerHTML = '<div class="no-topics">No topics available.</div>';
        return;
    }

    topicsList.innerHTML = topics.map(topic => `
        <div class="col-md-4 mb-3">
            <div class="topic-card">
                <div class="topic-content">
                    <h4 class="topic-name">${topic.name}</h4>
                    <span class="question-count">${topic.questionCount || 0} question${topic.questionCount !== 1 ? 's' : ''}</span>
                </div>
                <button onclick="deleteTopic(${topic.id})" 
                    class="btn ${topic.questionCount > 0 ? 'btn-secondary' : 'btn-danger'} delete-btn"
                    ${topic.questionCount > 0 ? 'disabled title="Cannot delete topics with questions"' : ''}>
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

// Add error handling for topic loading
async function loadTopics() {
    try {
        const response = await fetch('/api/topics', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error('Failed to load topics');
        const topics = await response.json();
        displayTopics(topics);
    } catch (error) {
        console.error('Error loading topics:', error);
        const topicsList = document.getElementById('topicsList');
        topicsList.innerHTML = '<div class="error-message">Error loading topics. Please try again later.</div>';
    }
}

// Add this function to load public banks
async function loadPublicBanks() {
    try {
        const response = await fetch('/api/questionbanks/public', {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load public banks');
        }

        const banks = await response.json();
        displayPublicBanks(banks);
    } catch (error) {
        console.error('Error loading public banks:', error);
    }
}
function displayPublicBanks(banks) {
    const publicBanksList = document.getElementById('publicBanksList');
    if (!banks || banks.length === 0) {
        publicBanksList.innerHTML = '<p class="no-banks-message">No public question banks available.</p>';
        return;
    }

    publicBanksList.innerHTML = banks.map(bank => `
        <div class="bank-card">
            <div class="bank-header">
                <h3>${bank.title}</h3>
                <div class="bank-meta">
                    <span class="owner-label">Created by: ${bank.owner.username}</span>
                    <span class="question-count">${bank.questions.length} questions</span>
                </div>
            </div>
            ${bank.description ? `
                <div class="bank-description">
                    ${bank.description}
                </div>
            ` : ''}
            <div class="bank-actions">
                <button onclick="openQuestionBank(${bank.id})" class="open-btn">View Bank</button>
            </div>
        </div>
    `).join('');
}

// Add this to your initialization code
document.addEventListener('DOMContentLoaded', () => {
    loadQuestionBanks();
    loadPublicBanks();
    loadQuizzes();
});

async function togglePublish(bankId) {
    try {
        const response = await fetch(`/api/questionbanks/${bankId}/publish`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to toggle publish status');
        }

        // Instead of reloading all banks, just update the current bank's status
        const updatedBank = await response.json();
        const bankIndex = currentBanks.findIndex(b => b.id === bankId);
        if (bankIndex !== -1) {
            currentBanks[bankIndex].published = updatedBank.published;
            displayBanks(currentBanks); // Re-render with the same order
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to update publish status: ' + error.message);
    }
}

// Add this to prevent event bubbling for the action buttons
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('banksList').addEventListener('click', (e) => {
        if (e.target.matches('button')) {
            e.stopPropagation();
        }
    });
});


