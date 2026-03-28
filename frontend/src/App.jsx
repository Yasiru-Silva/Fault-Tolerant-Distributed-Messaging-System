import { useEffect, useMemo, useState } from 'react'

const DEFAULT_NODES = [
  { id: 'node-1', url: 'http://localhost:8081' },
  { id: 'node-2', url: 'http://localhost:8082' },
  { id: 'node-3', url: 'http://localhost:8083' }
]

function sortMessages(messages) {
  return [...messages].sort((a, b) => {
    if (a.lamportTimestamp !== b.lamportTimestamp) {
      return a.lamportTimestamp - b.lamportTimestamp
    }
    const timeA = new Date(a.creationTime).getTime()
    const timeB = new Date(b.creationTime).getTime()
    if (timeA !== timeB) {
      return timeA - timeB
    }
    return String(a.id).localeCompare(String(b.id))
  })
}

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options)

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `Request failed: ${response.status}`)
  }

  return response.json()
}

/** Try each known node so cluster metadata still loads if node-1 (or the leader) is down. */
async function fetchLeaderAndNodesFromAnyNode() {
  let lastError = null

  for (const node of DEFAULT_NODES) {
    try {
      const [leader, nodes] = await Promise.all([
        fetchJson(`${node.url}/leader`),
        fetchJson(`${node.url}/nodes`)
      ])
      return { leader, nodes }
    } catch (err) {
      lastError = err
    }
  }

  throw lastError ?? new Error('No reachable node for cluster metadata')
}

export default function App() {
  const [selectedNode, setSelectedNode] = useState(DEFAULT_NODES[0].url)
  const [senderId, setSenderId] = useState('user1')
  const [content, setContent] = useState('')
  const [messagesByNode, setMessagesByNode] = useState({})
  const [leader, setLeader] = useState(null)
  const [nodes, setNodes] = useState([])
  const [statusCards, setStatusCards] = useState([])
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const selectedNodeLabel = useMemo(
    () => DEFAULT_NODES.find((node) => node.url === selectedNode)?.id ?? selectedNode,
    [selectedNode]
  )

  const messageCountsByNode = useMemo(() => {
    const counts = {}
    for (const node of DEFAULT_NODES) {
      counts[node.id] = messagesByNode[node.id]?.messages?.length ?? 0
    }
    return counts
  }, [messagesByNode])

  const loadAll = async ({ silent = false } = {}) => {
    if (silent) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }

    try {
      const [clusterRes, healthRes, messagesPerNodeRes] = await Promise.allSettled([
        fetchLeaderAndNodesFromAnyNode(),
        Promise.all(
          DEFAULT_NODES.map(async (node) => {
            try {
              const health = await fetchJson(`${node.url}/health`)
              return { ...node, online: true, health }
            } catch (nodeError) {
              return { ...node, online: false, error: nodeError.message }
            }
          })
        ),
        Promise.all(
          DEFAULT_NODES.map(async (node) => {
            try {
              const raw = await fetchJson(`${node.url}/messages`)
              return { id: node.id, messages: sortMessages(raw), error: null }
            } catch (nodeError) {
              return { id: node.id, messages: [], error: nodeError.message }
            }
          })
        )
      ])

      if (clusterRes.status === 'fulfilled') {
        setLeader(clusterRes.value.leader)
        setNodes(clusterRes.value.nodes)
      } else {
        setLeader(null)
        setNodes([])
      }

      if (healthRes.status === 'fulfilled') {
        setStatusCards(healthRes.value)
      }

      if (messagesPerNodeRes.status === 'fulfilled') {
        const next = {}
        for (const row of messagesPerNodeRes.value) {
          next[row.id] = { messages: row.messages, error: row.error }
        }
        setMessagesByNode(next)
      }

      const firstRejected = [clusterRes, healthRes, messagesPerNodeRes].find(
        (result) => result.status === 'rejected'
      )

      if (firstRejected) {
        setError(firstRejected.reason?.message || 'Some dashboard data could not be loaded.')
      } else {
        setError('')
      }
    } catch (loadError) {
      setError(loadError.message || 'Failed to load dashboard data.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    loadAll()
    const interval = setInterval(() => loadAll({ silent: true }), 2500)
    return () => clearInterval(interval)
  }, [])

  const handleSend = async (event) => {
    event.preventDefault()
    setSuccess('')
    setError('')

    if (!senderId.trim() || !content.trim()) {
      setError('Sender ID and message content are required.')
      return
    }

    try {
      const sent = await fetchJson(`${selectedNode}/send`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          senderId: senderId.trim(),
          content: content.trim()
        })
      })

      setSuccess(`Message sent via ${selectedNodeLabel}. Stored as ${sent.id}.`)
      setContent('')
      await loadAll({ silent: true })
    } catch (sendError) {
      setError(sendError.message || 'Failed to send message.')
    }
  }

  return (
    <div className="page">
      <aside className="sidebar">
        <div className="brandCard">
          <p className="eyebrow">Distributed Systems Demo</p>
          <h1>Fault-Tolerant Chat UI</h1>
          <p className="muted">
            Frontend for demonstrating leader election, replication, failover, and message ordering.
          </p>
        </div>

        <div className="panel">
          <h2>Leader</h2>
          {leader ? (
            <div className="leaderCard">
              <strong>{leader.nodeId}</strong>
              <span>{leader.url}</span>
              <span className="badge badgeLeader">{leader.role}</span>
            </div>
          ) : (
            <p className="muted">Leader not available yet.</p>
          )}
        </div>

        <div className="panel">
          <div className="panelHeader">
            <h2>Cluster Status</h2>
            <button className="ghostButton" onClick={() => loadAll()} disabled={loading || refreshing}>
              {refreshing ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
          <div className="nodeList">
            {statusCards.map((node) => (
              <div key={node.id} className="nodeCard">
                <div>
                  <strong>{node.id}</strong>
                  <p>{node.url}</p>
                </div>
                <span className={`badge ${node.online ? 'badgeOnline' : 'badgeOffline'}`}>
                  {node.online ? node.health.role : 'OFFLINE'}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="panel">
          <h2>Discovered Nodes</h2>
          <div className="nodeList compact">
            {nodes.length === 0 ? (
              <p className="muted">No active nodes found.</p>
            ) : (
              nodes.map((node) => (
                <div key={node.nodeId} className="nodeCard compactCard">
                  <div>
                    <strong>{node.nodeId}</strong>
                    <p>{node.url}</p>
                  </div>
                  <span className={`badge ${node.leader ? 'badgeLeader' : 'badgeFollower'}`}>
                    {node.role}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </aside>

      <main className="mainContent">
        <section className="panel heroPanel">
          <div>
            <p className="eyebrow">Live Messaging</p>
            <h2>Send through any node</h2>
            <p className="muted">
              Followers forward writes to the leader. Each column is that node’s message list; the page polls every few
              seconds so when you send through one server, the other two update live as replication catches up.
            </p>
          </div>
          <div className="heroMeta heroMetaWrap">
            <span className="metaChip">Send via: {selectedNodeLabel}</span>
            {DEFAULT_NODES.map((node) => (
              <span key={node.id} className="metaChip">
                {node.id}: {messageCountsByNode[node.id] ?? '—'}
              </span>
            ))}
          </div>
        </section>

        <section className="messagingLayout">
          <div className="panel composerPanel">
            <h2>Compose Message</h2>
            <form onSubmit={handleSend} className="formStack">
              <label>
                Sender ID
                <input
                  value={senderId}
                  onChange={(event) => setSenderId(event.target.value)}
                  placeholder="Enter sender ID"
                />
              </label>

              <label>
                Send Through Node
                <select value={selectedNode} onChange={(event) => setSelectedNode(event.target.value)}>
                  {DEFAULT_NODES.map((node) => (
                    <option key={node.id} value={node.url}>
                      {node.id} — {node.url}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Message
                <textarea
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                  placeholder="Type your message"
                  rows={5}
                />
              </label>

              <button className="primaryButton" type="submit">
                Send Message
              </button>
            </form>

            {success && <div className="notice success">{success}</div>}
            {error && <div className="notice error">{error}</div>}
          </div>

          <div className="nodeMessagesRow">
            {DEFAULT_NODES.map((node) => {
              const row = messagesByNode[node.id]
              const list = row?.messages ?? []
              const nodeError = row?.error

              return (
                <div key={node.id} className="panel messagePanel nodeMessagePanel">
                  <div className="panelHeader nodePanelHeader">
                    <div>
                      <h2>{node.id}</h2>
                      <p className="muted smallText nodeUrlLine">{node.url}</p>
                    </div>
                    <span className="metaChip metaChipSmall">{list.length} msgs</span>
                  </div>

                  {nodeError ? (
                    <div className="emptyState emptyStateError">{nodeError}</div>
                  ) : loading ? (
                    <div className="emptyState">Loading…</div>
                  ) : list.length === 0 ? (
                    <div className="emptyState">No messages yet.</div>
                  ) : (
                    <div className="messageList">
                      {list.map((message) => (
                        <article key={`${node.id}-${message.id}`} className="messageBubble">
                          <div className="messageTopRow">
                            <strong>{message.senderId}</strong>
                            <span className="badge badgeLamport">Lamport {message.lamportTimestamp}</span>
                          </div>
                          <p>{message.content}</p>
                          <div className="messageMeta">
                            <span>{new Date(message.creationTime).toLocaleString()}</span>
                            <span className="mono">{message.id}</span>
                          </div>
                        </article>
                      ))}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </section>
      </main>
    </div>
  )
}
