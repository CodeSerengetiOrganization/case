import { useState } from 'react'
import './SpikeManager.css'

const PRODUCTS = ['Ford APCU-01', 'GM-BCM-01', 'HONDA-OHC-01']

function failedTestItemText(testData) {
  if (!testData) {
    return ''
  }
  return Object.keys(testData).join(', ')
}

function stationSetupText(setup) {
  if (!setup) {
    return ''
  }
  return [setup.station_id, setup.rack_id, setup.fixture_id, setup.nest_number]
    .filter((part) => part !== undefined && part !== null && part !== '')
    .join(' / ')
}

export function SpikeManager() {
  const [productName, setProductName] = useState('')
  const [stored, setStored] = useState(false)
  const [storedProductName, setStoredProductName] = useState('')
  const [rows, setRows] = useState([])
  const [error, setError] = useState('')

  async function reportSpike() {
    setError('')
    setStored(false)
    setStoredProductName('')
    setRows([])
    try {
      const response = await fetch(`${import.meta.env.VITE_API_URL}/apis/spikes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ product_name: productName }),
      })
      const data = await response.json()
      if (data.id == null) {
        setError(data.message ?? '')
        return
      }
      setStored(true)
      setStoredProductName(data.product_name ?? '')
      setRows(data.failed_manufacturing_results ?? [])
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
      {error ? <p className="message error">{error}</p> : null}
      {stored ? <p className="message ok">The spike is reported and stored.</p> : null}
      {stored && rows.length > 0 ? (
        <section className="panel">
          <h2>Copied failed results</h2>
          <table>
            <thead>
              <tr>
                <th>Product name</th>
                <th>Serial number</th>
                <th>Failed test item</th>
                <th>Op code</th>
                <th>Station setup</th>
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
                  <td>{stationSetupText(row.station_setup)}</td>
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
