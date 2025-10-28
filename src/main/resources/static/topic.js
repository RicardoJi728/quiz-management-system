/**
 * Renders mathematical expressions using MathJax
 * This function is called after displaying questions to ensure proper rendering of math content
 */
function renderMathJax() {
    if (window.MathJax) {
        MathJax.typesetPromise && MathJax.typesetPromise();
    }
}

// Get authentication token from localStorage
const token = localStorage.getItem('token');
// Redirect to login page if no token is found
if (!token) {
    localStorage.clear();
    window.location.href = '/';
}

// Get topic and bank IDs from URL parameters
const urlParams = new URLSearchParams(window.location.search);
const topicId = urlParams.get('topicId');
const bankId = urlParams.get('bankId');

/**
 * Loads the details of the current topic
 * Fetches topic information from the server and updates the page title
 */
async function loadTopicDetails() {
    try {
        const response = await fetch(`/api/topics/${topicId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const topic = await response.json();
        document.getElementById('topicTitle').textContent = `Topic: ${topic.name}`;
        loadTopicQuestions();
    } catch (error) {
        console.error('Error loading topic details:', error);
    }
}

/**
 * Loads all questions associated with the current topic
 * Fetches questions from the server and displays them
 */
async function loadTopicQuestions() {
    try {
        const response = await fetch(`/api/questions/bank/${bankId}/topic/${topicId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const questions = await response.json();
        displayQuestions(questions);
    } catch (error) {
        console.error('Error loading topic questions:', error);
    }
}

/**
 * Displays the list of questions for the current topic
 * @param {Array} questions - Array of question objects to display
 */
function displayQuestions(questions) {
    const questionsList = document.getElementById('topicQuestionsList');
    // Show message if no questions exist
    if (!questions || questions.length === 0) {
        questionsList.innerHTML = '<p class="no-questions">No questions added yet.</p>';
        return;
    }
    
    // Generate HTML for each question
    questionsList.innerHTML = questions.map(q => `
        <div class="question-item">
            <!-- Question action buttons -->
            <div class="question-sidebar">
                <button onclick="editQuestion(${q.id})" class="action-btn edit-btn">Edit</button>
                <button onclick="toggleInfo(${q.id})" class="action-btn info-btn">Info</button>
                <button onclick="deleteQuestion(${q.id})" class="action-btn delete-btn">Delete</button>
            </div>

            <!-- Main question content -->
            <div class="question-main">
                <!-- Question metadata -->
                <div class="question-header">
                    <div class="question-meta">
                        <span class="question-type-badge">${q.questionType}</span>
                        <span class="level-badge level-${q.level}">${q.level}</span>
                        ${q.points ? `<span class="points-badge">${q.points} points</span>` : ''}
                        ${q.topic ? `<span class="topic-badge">${q.topic.name}</span>` : ''}
                    </div>
                </div>

                <!-- Question body with different layouts based on type -->
                <div class="question-body">
                    <!-- Question text with support for subsections -->
                    <div class="question-text">
                        ${q.questionType === 'WITH_SUBSECTIONS' ? `
                            <div class="main-question">
                                <p>${q.questionText}</p>
                            </div>
                            ${q.subQuestions && q.subQuestions.map((sub, index) => `
                                <div class="sub-question-item">
                                    <p class="sub-question-text">${String.fromCharCode(97 + index)}) ${sub.questionText}</p>
                                    <div class="mark-scheme">Mark Scheme: ${sub.markScheme}</div>
                                    <div class="sub-points">Points: ${sub.points}</div>
                                </div>
                            `).join('')}
                        ` : `
                            <p>${q.questionText || ''}</p>
                        `}
                    </div>

                    <!-- Multiple choice options if applicable -->
                    ${q.questionType === 'MULTIPLE_CHOICE' && q.options && q.options.length > 0 ? `
                        <div class="multiple-choice-options">
                            ${q.options.map((option, index) => `
                                <div class="option-line ${option.correct ? 'correct-option' : ''}">
                                    ${String.fromCharCode(65 + index)}. ${option.optionText}
                                </div>
                            `).join('')}
                        </div>
                    ` : ''}

                    <!-- Mark scheme for non-multiple choice questions -->
                    ${q.questionType !== 'MULTIPLE_CHOICE' && q.questionType !== 'WITH_SUBSECTIONS' && q.markScheme ? `
                        <div class="mark-scheme">
                            <h4>Mark Scheme</h4>
                            <p>${q.markScheme}</p>
                        </div>
                    ` : ''}
                </div>

                <!-- Additional information panel -->
                <div id="info-${q.id}" class="info-panel" style="display: none;">
                    <div class="info-content">
                        ${q.publicationDate ? `
                            <div class="info-item">
                                <span class="info-label">Published:</span>
                                <span class="info-value">${new Date(q.publicationDate).toLocaleDateString()}</span>
                            </div>
                        ` : ''}
                        ${q.additionalInfo ? `
                            <div class="info-item">
                                <span class="info-label">Additional Info:</span>
                                <span class="info-value">${q.additionalInfo}</span>
                            </div>
                        ` : ''}
                    </div>
                </div>
            </div>
        </div>
    `).join('');

    // Render any mathematical expressions in the questions
    renderMathJax();
}

/**
 * Displays multiple choice options for a question
 * @param {Object} question - The question object containing options
 * @returns {string} HTML string for the options
 */
function displayMultipleChoiceOptions(question) {
    if (!question.options) return '';
    return `
        <div class="options-list">
            ${question.options.map((option, index) => `
                <div class="option">
                    <span class="option-label">${String.fromCharCode(65 + index)}.</span>
                    <span class="option-text">${option}</span>
                    ${question.correctOption === index ? '<span class="correct-answer">✓</span>' : ''}
                </div>
            `).join('')}
        </div>
    `;
}

/**
 * Deletes a question after confirmation
 * @param {number} questionId - The ID of the question to delete
 */
async function deleteQuestion(questionId) {
    if (!confirm('Are you sure you want to delete this question?')) return;
    
    try {
        const response = await fetch(`/api/questions/${questionId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            loadTopicQuestions();
        } else {
            console.error('Failed to delete question');
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

/**
 * Toggles the visibility of the additional information panel for a question
 * @param {number} questionId - The ID of the question whose info panel to toggle
 */
function toggleInfo(questionId) {
    const infoPanel = document.getElementById(`info-${questionId}`);
    if (infoPanel) {
        infoPanel.style.display = infoPanel.style.display === 'none' ? 'block' : 'none';
    }
}

/**
 * Redirects to the question bank page in edit mode for a specific question
 * @param {number} questionId - The ID of the question to edit
 */
function editQuestion(questionId) {
    window.location.href = `/questionbank.html?id=${bankId}&editQuestion=${questionId}`;
}

// Load topic details when the page loads
document.addEventListener('DOMContentLoaded', loadTopicDetails);