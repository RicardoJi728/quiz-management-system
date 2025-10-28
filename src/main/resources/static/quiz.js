// Get authentication token from localStorage
const token = localStorage.getItem('token');
// Redirect to login page if no token is found
if (!token) {
    window.location.href = '/';
}

// Get question bank ID from URL parameters
const urlParams = new URLSearchParams(window.location.search);
const bankId = urlParams.get('bankId');

/**
 * Shows the random quiz form and hides other quiz forms
 * Updates the active state of the quiz mode buttons
 */
function showRandomQuiz() {
    document.getElementById('randomQuizForm').style.display = 'block';
    document.getElementById('customQuizForm').style.display = 'none';
    document.getElementById('fullQuizForm').style.display = 'none';
    document.getElementById('practiceQuizForm').style.display = 'none';
    
    // Update active state of buttons
    setActiveButton('Random Quiz');
}

/**
 * Shows the custom quiz form and loads available questions
 * Hides other quiz forms and updates button states
 */
function showCustomQuiz() {
    document.getElementById('randomQuizForm').style.display = 'none';
    document.getElementById('customQuizForm').style.display = 'block';
    document.getElementById('fullQuizForm').style.display = 'none';
    document.getElementById('practiceQuizForm').style.display = 'none';
    loadQuestionsForCustomQuiz();
}

/**
 * Shows the full quiz form and hides other quiz forms
 */
function showFullQuiz() {
    document.getElementById('randomQuizForm').style.display = 'none';
    document.getElementById('customQuizForm').style.display = 'none';
    document.getElementById('fullQuizForm').style.display = 'block';
    document.getElementById('practiceQuizForm').style.display = 'none';
}

/**
 * Shows the practice quiz form and hides other quiz forms
 * Updates the active state of the quiz mode buttons
 */
function showPracticeQuiz() {
    document.getElementById('randomQuizForm').style.display = 'none';
    document.getElementById('customQuizForm').style.display = 'none';
    document.getElementById('fullQuizForm').style.display = 'none';
    document.getElementById('practiceQuizForm').style.display = 'block';
    setActiveButton('Practice');
}

/**
 * Updates the active state of quiz mode buttons
 * @param {string} activeMode - The name of the active quiz mode
 */
function setActiveButton(activeMode) {
    const buttons = document.querySelectorAll('.quiz-mode-buttons button');
    buttons.forEach(button => {
        button.classList.toggle('active', button.textContent === activeMode);
    });
}

/**
 * Loads all questions from the question bank for custom quiz creation
 * Fetches questions from the server and displays them grouped by topic and level
 */
async function loadQuestionsForCustomQuiz() {
    try {
        // Log the bank ID being loaded for debugging purposes
        console.log('Loading questions for bank:', bankId);
        
        // Send request to fetch all questions in the specified bank
        const response = await fetch(`/api/questions/bank/${bankId}/all`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'  // Set JSON content type
            }
        });

        // Check if the request was unsuccessful and handle errors
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to load questions');
        }

        // Parse the JSON response containing the questions
        const questions = await response.json();
        // Log loaded questions for debugging
        console.log('Loaded questions:', questions);
        
        // Verify we received actual questions
        if (!questions || questions.length === 0) {
            throw new Error('No questions found in this bank');
        }
        
        // Display the questions grouped by topic and difficulty level
        displayQuestionsGrouped(questions);
    } catch (error) {
        // Log and display any errors that occurred during the fetch operation
        console.error('Error loading questions:', error);
        alert('Failed to load questions: ' + error.message);
    }
}

/**
 * Displays questions grouped by topic and difficulty level
 * @param {Array} questions - Array of question objects to display
 */
function displayQuestionsGrouped(questions) {
    const questionsList = document.getElementById('questionsList');
    const groupedQuestions = groupQuestionsByTopicAndLevel(questions);
    updateQuestionCounts(questions);
    
    let html = '';
    for (const topic in groupedQuestions) {
        html += `<div class="topic-section">
            <h4>${topic}</h4>`;
        
        // Standard Level Questions
        if (groupedQuestions[topic].STANDARD?.length > 0) {
            html += `<div class="level-section">
                <h5>Standard Level (${groupedQuestions[topic].STANDARD.length} questions)</h5>
                ${groupedQuestions[topic].STANDARD.map(q => createQuestionCheckbox(q)).join('')}
            </div>`;
        }
        
        // Higher Level Questions
        if (groupedQuestions[topic].HIGHER?.length > 0) {
            html += `<div class="level-section">
                <h5>Higher Level (${groupedQuestions[topic].HIGHER.length} questions)</h5>
                ${groupedQuestions[topic].HIGHER.map(q => createQuestionCheckbox(q)).join('')}
            </div>`;
        }
        
        html += '</div>';
    }
    
    questionsList.innerHTML = html;
    setupQuestionSearch();
    setupSelectionCounter();
    renderMathJax();
}

/**
 * Updates the question count displays for standard and higher level questions
 * @param {Array} questions - Array of question objects
 */
function updateQuestionCounts(questions) {
    const standardCount = questions.filter(q => q.level === 'STANDARD').length;
    const higherCount = questions.filter(q => q.level === 'HIGHER').length;
    
    document.getElementById('standardCount').textContent = `(${standardCount} questions)`;
    document.getElementById('higherCount').textContent = `(${higherCount} questions)`;
}

/**
 * Sets up the question search functionality
 * Filters questions based on user input
 */
function setupQuestionSearch() {
    const searchBox = document.getElementById('searchQuestions');
    searchBox.addEventListener('input', (e) => {
        const searchTerm = e.target.value.toLowerCase();
        const questions = document.querySelectorAll('.question-checkbox');
        
        questions.forEach(q => {
            const text = q.querySelector('.question-text').textContent.toLowerCase();
            q.style.display = text.includes(searchTerm) ? '' : 'none';
        });
    });
}

/**
 * Sets up the question selection counter
 * Updates the display when questions are selected/deselected
 */
function setupSelectionCounter() {
    const checkboxes = document.querySelectorAll('input[name="selectedQuestions"]');
    const countDisplay = document.getElementById('selectedCount');
    
    checkboxes.forEach(cb => {
        cb.addEventListener('change', () => {
            const selectedCount = document.querySelectorAll('input[name="selectedQuestions"]:checked').length;
            countDisplay.textContent = selectedCount;
        });
    });
}

/**
 * Groups questions by topic and difficulty level
 * @param {Array} questions - Array of question objects
 * @returns {Object} Grouped questions object
 */
function groupQuestionsByTopicAndLevel(questions) {
    return questions.reduce((acc, q) => {
        const topicName = q.topic?.name || 'Uncategorized';
        if (!acc[topicName]) {
            acc[topicName] = {};
        }
        if (!acc[topicName][q.level]) {
            acc[topicName][q.level] = [];
        }
        acc[topicName][q.level].push(q);
        return acc;
    }, {});
}

/**
 * Creates a checkbox element for a question
 * @param {Object} question - The question object
 * @returns {string} HTML string for the question checkbox
 */
function createQuestionCheckbox(question) {
    // Calculate total points for questions with subsections
    let totalPoints = question.points || 0;
    if (question.questionType === 'WITH_SUBSECTIONS' && question.subQuestions) {
        totalPoints = question.subQuestions.reduce((sum, sub) => sum + (sub.points || 0), 0);
    }

    return `
        <div class="question-checkbox">
            <label>
                <input type="checkbox" name="selectedQuestions" value="${question.id}">
                <div class="question-content">
                    <div class="question-main-text">
                        <span class="question-text">${question.questionText}</span>
                        <span class="question-points">(${totalPoints} points)</span>
                    </div>
                    ${question.questionType === 'WITH_SUBSECTIONS' && question.subQuestions ? `
                        <div class="sub-questions-preview">
                            ${question.subQuestions.map((sub, index) => `
                                <div class="sub-question-preview">
                                    <span class="sub-identifier">${String.fromCharCode(97 + index)})</span>
                                    <span class="sub-question-text">${sub.questionText}</span>
                                    <span class="sub-question-points">(${sub.points} points)</span>
                                </div>
                            `).join('')}
                        </div>
                    ` : ''}
                </div>
            </label>
        </div>
    `;
}

/**
 * Starts a custom quiz with selected questions
 * Validates input and creates quiz on the server
 */
async function startCustomQuiz() {
    const selectedQuestions = Array.from(document.querySelectorAll('input[name="selectedQuestions"]:checked'))
        .map(cb => parseInt(cb.value));
    
    if (selectedQuestions.length === 0) {
        alert('Please select at least one question');
        return;
    }

    const timeLimit = parseInt(document.getElementById('customTimeLimit').value);
    if (timeLimit < 1) {
        alert('Time limit must be at least 1 minute');
        return;
    }

    try {
        const response = await fetch('/api/quizzes/create/custom', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                bankId: parseInt(bankId),
                timeLimit: timeLimit,
                questionIds: selectedQuestions
            })
        });

        if (!response.ok) {
            throw new Error('Failed to create custom quiz');
        }

        const quiz = await response.json();
        window.location.href = `take-quiz.html?quizId=${quiz.id}`;
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to create custom quiz: ' + error.message);
    }
}

/**
 * Starts a full quiz with selected difficulty levels
 * Validates input and creates quiz on the server
 */
async function startFullQuiz() {
    const selectedLevels = Array.from(document.querySelectorAll('input[name="fullQuizLevels"]:checked'))
        .map(cb => cb.value);
    
    if (selectedLevels.length === 0) {
        alert('Please select at least one difficulty level');
        return;
    }

    const timeLimit = parseInt(document.getElementById('fullTimeLimit').value);
    if (timeLimit < 1) {
        alert('Time limit must be at least 1 minute');
        return;
    }

    try {
        const response = await fetch('/api/quizzes/create/full', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                bankId: parseInt(bankId),
                timeLimit: timeLimit,
                levels: selectedLevels
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to create full quiz');
        }

        const quiz = await response.json();
        window.location.href = `take-quiz.html?quizId=${quiz.id}`;
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to create full quiz: ' + error.message);
    }
}

/**
 * Loads available topics for the question bank
 * Fetches topics from the server and displays them
 */
async function loadTopics() {
    try {
        const token = localStorage.getItem('token');
        if (!token) {
            window.location.href = '/';
            return;
        }

        const urlParams = new URLSearchParams(window.location.search);
        const bankId = urlParams.get('bankId');
        
        const response = await fetch(`/api/questions/bank/${bankId}/topics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load topics');
        }

        const topics = await response.json();
        displayTopics(topics);
    } catch (error) {
        console.error('Error loading topics:', error);
        alert('Failed to load topics: ' + error.message);
    }
}

/**
 * Displays available topics with their question counts
 * @param {Array} topics - Array of topic objects
 */
function displayTopics(topics) {
    const topicsList = document.getElementById('topicsList');
    
    const levelsHtml = `
        <div class="form-group">
            <label>Difficulty Levels:</label>
            <div class="checkbox-group">
                <label>
                    <input type="checkbox" name="levels" value="STANDARD" checked> Standard
                </label>
                <label>
                    <input type="checkbox" name="levels" value="HIGHER"> Higher
                </label>
            </div>
        </div>
    `;

    const topicsHtml = topics && topics.length > 0 
        ? `
            <div class="form-group">
                <label>Select Topics (Optional - leave all unchecked to include all topics):</label>
                <div class="checkbox-group">
                    ${topics.map(topic => `
                        <label>
                            <input type="checkbox" name="topics" value="${topic.id}">
                            ${topic.name} (${topic.questionCount} questions)
                        </label>
                    `).join('')}
                </div>
            </div>
        `
        : '<p>No topics available</p>';

    topicsList.innerHTML = levelsHtml + topicsHtml;
}

// Initialize quiz page when loaded
document.addEventListener('DOMContentLoaded', () => {
    loadTopics();
    // Show random quiz form by default
    showRandomQuiz();

    // Handle quiz configuration form submission
    document.getElementById('quizConfigForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        try {
            // Validate time limit
            const timeLimit = parseInt(document.getElementById('timeLimit').value);
            if (timeLimit < 1) {
                throw new Error('Time limit must be at least 1 minute');
            }

            // Validate number of questions
            const numberOfQuestions = parseInt(document.getElementById('numberOfQuestions').value);
            if (numberOfQuestions < 1) {
                throw new Error('Number of questions must be at least 1');
            }

            // Validate levels selection
            const selectedLevels = Array.from(document.querySelectorAll('input[name="levels"]:checked'))
                .map(cb => cb.value);
            if (selectedLevels.length === 0) {
                throw new Error('Please select at least one difficulty level');
            }

            // Get selected topics (optional) - send null if none selected
            const selectedTopics = Array.from(document.querySelectorAll('input[name="topics"]:checked'))
                .map(cb => parseInt(cb.value));

            const urlParams = new URLSearchParams(window.location.search);
            const bankId = parseInt(urlParams.get('bankId'));

            const requestBody = {
                bankId: bankId,
                timeLimit: timeLimit,
                numberOfQuestions: numberOfQuestions,
                levels: selectedLevels,
                topicIds: selectedTopics.length > 0 ? selectedTopics : null
            };

            console.log('Creating quiz with config:', requestBody);

            const response = await fetch('/api/quizzes/create', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                if (!response.ok) {
                    throw new Error(data.error || 'Failed to create quiz');
                }
                window.location.href = `take-quiz.html?quizId=${data.id}`;
            } else {
                const text = await response.text();
                throw new Error(text || 'Failed to create quiz');
            }

        } catch (error) {
            console.error('Error creating quiz:', error);
            alert(error.message);
        }
    });
});

/**
 * Starts a practice quiz with selected difficulty levels
 * Validates input and creates quiz on the server
 */
async function startPracticeQuiz() {
    const timeLimit = parseInt(document.getElementById('practiceTimeLimit').value);
    const selectedLevels = Array.from(document.querySelectorAll('input[name="practiceLevels"]:checked'))
        .map(cb => cb.value);

    if (!timeLimit || timeLimit < 1) {
        alert('Please enter a valid time limit (minimum 1 minute)');
        return;
    }

    if (selectedLevels.length === 0) {
        alert('Please select at least one difficulty level');
        return;
    }

    try {
        const response = await fetch('/api/quizzes/practice', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                bankId: parseInt(bankId),
                timeLimit: timeLimit,
                levels: selectedLevels
            })
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to create practice quiz');
        }

        const quiz = await response.json();
        window.location.href = `take-quiz.html?quizId=${quiz.id}`;
    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    }
}

/**
 * Renders mathematical expressions using MathJax
 * This function is called after displaying questions to ensure proper rendering of math content
 */
function renderMathJax() {
    if (window.MathJax) {
        MathJax.typesetPromise && MathJax.typesetPromise();
    }
}