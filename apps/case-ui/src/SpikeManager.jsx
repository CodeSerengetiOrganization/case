import { useState } from 'react'
import './SpikeManager.css'

const PRODUCTS = ['Ford APCU-01', 'GM-BCM-01', 'HONDA-OHC-01']

function failedTestItemText(testData) {
  if (!testData) {
    return ''
  }
  return Object.keys(testData).join(', ')
}

export function SpikeManager() {
  const [productName, setProductName] = useState('')
  const [stored, setStored] = useState(false)
  const [alreadyReported, setAlreadyReported] = useState(false)
  const [storedProductName, setStoredProductName] = useState('')
  const [rows, setRows] = useState([])
  const [info, setInfo] = useState('')
  const [error, setError] = useState('')

  async function reportSpike() {
    setError('')
    setInfo('')
    setStored(false)
    setAlreadyReported(false)
    setStoredProductName('')
    setRows([])
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/apis/spikes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ product_name: productName }),
      })
      let data = {}
      try {
        data = await response.json()
      } catch {
        data = {}
      }
      if (response.status >= 400) {
        setError(data.message ?? 'The report failed.')
        return
      }
      if (data.id == null) {
        setInfo(data.message ?? '')
        return
      }
      if (data.repeated === true) {
        setStoredProductName(data.product_name ?? '')
        setRows(data.failed_manufacturing_results ?? [])
        setAlreadyReported(true)
        return
      }
      if (data.repeated === false) {
        setStoredProductName(data.product_name ?? '')
        setRows(data.failed_manufacturing_results ?? [])
        setStored(true)
        return
      }
      setError('The report response is incomplete. Please contact the support team.')
    } catch {
      setError('The report failed.')
    }
  }

  return (
    <main className="page">
      <h1>Report spike</h1>
      <label className="field">
        Product name
        <div className="input-row">
          <select
            id="product-name"
            value={productName}
            onChange={(event) => setProductName(event.target.value)}
          >
            <option value=""></option>
            {PRODUCTS.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>
          <button
            type="button"
            className="submit"
            onClick={reportSpike}
            disabled={productName === ''}
          >
            Report spike
          </button>
        </div>
      </label>
      {info ? <p className="message">{info}</p> : null}
      {error ? <p className="message error">{error}</p> : null}
      {alreadyReported ? (
        <p className="message ok">
          This spike was already reported. The operator does not need to report it again.
        </p>
      ) : null}
      {stored ? <p className="message ok">The spike is reported and stored.</p> : null}
      {(stored || alreadyReported) && rows.length > 0 ? (
        <section className="panel">
          <h2>Copied failed results</h2>
          <table>
            <thead>
              <tr>
                <th>Product name</th>
                <th>Serial number</th>
                <th>Failed test item</th>
                <th>Op code</th>
                <th>Station id</th>
                <th>Rack id</th>
                <th>Fixture id</th>
                <th>Nest number</th>
                <th>Operator id</th>
                <th>Shift code</th>
                <th>Created at</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{storedProductName}</td>
                  <td>{row.serial_number}</td>
                  <td>{failedTestItemText(row.test_data_json)}</td>
                  <td>{row.op_code}</td>
                  <td>{row.station_id}</td>
                  <td>{row.rack_id}</td>
                  <td>{row.fixture_id}</td>
                  <td>{row.nest_number}</td>
                  <td>{row.operator_id}</td>
                  <td>{row.shift_code}</td>
                  <td>{row.created_at}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}
    </main>
  )
}
